package com.fridgechef.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fridgechef.TestData;
import com.fridgechef.domain.Ingredient;
import com.fridgechef.domain.MeasureUnit;
import com.fridgechef.domain.ShoppingListItem;
import com.fridgechef.dto.IngredientRequirement;
import com.fridgechef.dto.RecipeMatch;
import com.fridgechef.dto.ShoppingItemForm;
import com.fridgechef.dto.ShoppingListItemView;
import com.fridgechef.exception.ResourceNotFoundException;
import com.fridgechef.mapper.ShoppingListItemMapper;
import com.fridgechef.repository.ShoppingListItemRepository;

@ExtendWith(MockitoExtension.class)
class ShoppingListServiceTest {

    @Mock
    private ShoppingListItemRepository shoppingListItemRepository;

    @Mock
    private IngredientService ingredientService;

    @Mock
    private RecipeMatchingService recipeMatchingService;

    @Mock
    private FridgeService fridgeService;

    private ShoppingListService service;

    private final Ingredient eggs = TestData.ingredient(1L, "Eggs", MeasureUnit.PIECE);
    private final Ingredient milk = TestData.ingredient(2L, "Milk", MeasureUnit.MILLILITER);

    @BeforeEach
    void setUp() {
        service = new ShoppingListService(shoppingListItemRepository, ingredientService, recipeMatchingService,
                fridgeService, new ShoppingListItemMapper());
    }

    @Test
    void findAllMapsItems() {
        when(shoppingListItemRepository.findAllByOrderByPurchasedAscIdAsc())
                .thenReturn(List.of(TestData.shoppingItem(1L, eggs, 10)));

        assertThat(service.findAll())
                .containsExactly(new ShoppingListItemView(1L, "Eggs", 10, MeasureUnit.PIECE, false));
    }

    @Test
    void addCreatesNewItemWhenIngredientIsNotOnTheList() {
        when(ingredientService.getEntity(1L)).thenReturn(eggs);
        when(shoppingListItemRepository.findByIngredientIdAndPurchasedFalse(1L)).thenReturn(Optional.empty());

        service.add(form(1L, 6.0));

        ArgumentCaptor<ShoppingListItem> saved = ArgumentCaptor.forClass(ShoppingListItem.class);
        verify(shoppingListItemRepository).save(saved.capture());
        assertThat(saved.getValue().getIngredient()).isSameAs(eggs);
        assertThat(saved.getValue().getQuantity()).isEqualTo(6);
    }

    @Test
    void addIncreasesQuantityOfExistingOpenItem() {
        ShoppingListItem existing = TestData.shoppingItem(5L, eggs, 4);
        when(ingredientService.getEntity(1L)).thenReturn(eggs);
        when(shoppingListItemRepository.findByIngredientIdAndPurchasedFalse(1L)).thenReturn(Optional.of(existing));

        service.add(form(1L, 6.0));

        assertThat(existing.getQuantity()).isEqualTo(10);
        verify(shoppingListItemRepository, never()).save(any());
    }

    @Test
    void addMissingIngredientsAddsOnlyWhatTheRecipeLacks() {
        List<IngredientRequirement> requirements = List.of(
                new IngredientRequirement(1L, "Eggs", MeasureUnit.PIECE, 3, 5, false),
                new IngredientRequirement(2L, "Milk", MeasureUnit.MILLILITER, 300, 100, false));
        when(recipeMatchingService.getMatch(7L))
                .thenReturn(new RecipeMatch(7L, "Pancakes", "d", "s", 30, 50, 0, requirements));
        when(ingredientService.getEntity(2L)).thenReturn(milk);
        when(shoppingListItemRepository.findByIngredientIdAndPurchasedFalse(2L)).thenReturn(Optional.empty());

        int added = service.addMissingIngredients(7L);

        assertThat(added).isEqualTo(1);
        ArgumentCaptor<ShoppingListItem> saved = ArgumentCaptor.forClass(ShoppingListItem.class);
        verify(shoppingListItemRepository).save(saved.capture());
        assertThat(saved.getValue().getIngredient()).isSameAs(milk);
        assertThat(saved.getValue().getQuantity()).isEqualTo(200);
    }

    @Test
    void togglePurchasedFlipsTheFlag() {
        ShoppingListItem item = TestData.shoppingItem(5L, eggs, 4);
        when(shoppingListItemRepository.findById(5L)).thenReturn(Optional.of(item));

        service.togglePurchased(5L);

        assertThat(item.isPurchased()).isTrue();
    }

    @Test
    void removeDeletesItem() {
        ShoppingListItem item = TestData.shoppingItem(5L, eggs, 4);
        when(shoppingListItemRepository.findById(5L)).thenReturn(Optional.of(item));

        service.remove(5L);

        verify(shoppingListItemRepository).delete(item);
    }

    @Test
    void unknownItemCausesNotFound() {
        when(shoppingListItemRepository.findById(5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.togglePurchased(5L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Shopping list item with id 5 was not found");
    }

    @Test
    void purchasedItemsAreMovedToTheFridge() {
        ShoppingListItem boughtEggs = TestData.shoppingItem(1L, eggs, 10);
        ShoppingListItem boughtMilk = TestData.shoppingItem(2L, milk, 1000);
        List<ShoppingListItem> purchased = List.of(boughtEggs, boughtMilk);
        when(shoppingListItemRepository.findByPurchasedTrue()).thenReturn(purchased);

        int moved = service.movePurchasedToFridge();

        assertThat(moved).isEqualTo(2);
        verify(fridgeService).addPurchased(eggs, 10);
        verify(fridgeService).addPurchased(milk, 1000);
        verify(shoppingListItemRepository).deleteAll(purchased);
    }

    private static ShoppingItemForm form(Long ingredientId, Double quantity) {
        ShoppingItemForm form = new ShoppingItemForm();
        form.setIngredientId(ingredientId);
        form.setQuantity(quantity);
        return form;
    }
}
