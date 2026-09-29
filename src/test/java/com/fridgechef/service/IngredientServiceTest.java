package com.fridgechef.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fridgechef.TestData;
import com.fridgechef.domain.Ingredient;
import com.fridgechef.domain.MeasureUnit;
import com.fridgechef.dto.IngredientForm;
import com.fridgechef.dto.IngredientOverview;
import com.fridgechef.dto.IngredientUsage;
import com.fridgechef.dto.IngredientView;
import com.fridgechef.exception.DuplicateIngredientException;
import com.fridgechef.exception.IngredientInUseException;
import com.fridgechef.exception.ResourceNotFoundException;
import com.fridgechef.mapper.IngredientMapper;
import com.fridgechef.repository.FridgeItemRepository;
import com.fridgechef.repository.IngredientCount;
import com.fridgechef.repository.IngredientRepository;
import com.fridgechef.repository.RecipeRepository;
import com.fridgechef.repository.ShoppingListItemRepository;

@ExtendWith(MockitoExtension.class)
class IngredientServiceTest {

    @Mock
    private IngredientRepository ingredientRepository;

    @Mock
    private RecipeRepository recipeRepository;

    @Mock
    private FridgeItemRepository fridgeItemRepository;

    @Mock
    private ShoppingListItemRepository shoppingListItemRepository;

    private IngredientService ingredientService;

    @BeforeEach
    void setUp() {
        ingredientService = new IngredientService(ingredientRepository, recipeRepository, fridgeItemRepository,
                shoppingListItemRepository, new IngredientMapper());
    }

    @Test
    void findAllReturnsViewsSortedByRepository() {
        when(ingredientRepository.findAllByOrderByNameAsc()).thenReturn(List.of(
                TestData.ingredient(1L, "Eggs", MeasureUnit.PIECE),
                TestData.ingredient(2L, "Milk", MeasureUnit.MILLILITER)));

        assertThat(ingredientService.findAll())
                .extracting(IngredientView::name)
                .containsExactly("Eggs", "Milk");
    }

    @Test
    void createSavesNewIngredient() {
        when(ingredientRepository.existsByNameIgnoreCase("Butter")).thenReturn(false);
        when(ingredientRepository.save(any(Ingredient.class)))
                .thenReturn(TestData.ingredient(7L, "Butter", MeasureUnit.GRAM, 30));

        IngredientView created = ingredientService.create(form(" Butter "));

        assertThat(created).isEqualTo(new IngredientView(7L, "Butter", MeasureUnit.GRAM, 30));
    }

    @Test
    void createRejectsDuplicateName() {
        when(ingredientRepository.existsByNameIgnoreCase("Butter")).thenReturn(true);
        IngredientForm duplicate = form("Butter");

        assertThatThrownBy(() -> ingredientService.create(duplicate))
                .isInstanceOf(DuplicateIngredientException.class)
                .hasMessageContaining("Butter");
        verify(ingredientRepository, never()).save(any());
    }

    @Test
    void getEntityThrowsWhenMissing() {
        when(ingredientRepository.findById(42L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ingredientService.getEntity(42L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Ingredient with id 42 was not found");
    }

    @Test
    void findAllWithUsageCombinesCountsFromEveryTable() {
        when(ingredientRepository.findAllByOrderByNameAsc()).thenReturn(List.of(
                TestData.ingredient(1L, "Eggs", MeasureUnit.PIECE),
                TestData.ingredient(2L, "Honey", MeasureUnit.GRAM)));
        when(recipeRepository.countPerIngredient()).thenReturn(List.of(count(1L, 3)));
        when(fridgeItemRepository.countPerIngredient()).thenReturn(List.of(count(1L, 2)));
        when(shoppingListItemRepository.countPerIngredient()).thenReturn(List.of(count(1L, 1)));

        assertThat(ingredientService.findAllWithUsage())
                .extracting(entry -> entry.ingredient().name(), IngredientOverview::usage)
                .containsExactly(
                        tuple("Eggs", new IngredientUsage(3, 2, 1)),
                        tuple("Honey", new IngredientUsage(0, 0, 0)));
    }

    @Test
    void getOverviewReturnsIngredientWithItsUsage() {
        when(ingredientRepository.findById(1L)).thenReturn(Optional.of(TestData.ingredient(1L, "Milk", MeasureUnit.MILLILITER)));
        stubUsage(1L, 4, 1, 0);

        IngredientOverview overview = ingredientService.getOverview(1L);

        assertThat(overview.ingredient().name()).isEqualTo("Milk");
        assertThat(overview.usage()).isEqualTo(new IngredientUsage(4, 1, 0));
    }

    @Test
    void updateRenamesUsedIngredientWhenUnitStaysTheSame() {
        Ingredient butter = TestData.ingredient(3L, "Butter", MeasureUnit.GRAM, 30);
        when(ingredientRepository.findById(3L)).thenReturn(Optional.of(butter));
        when(ingredientRepository.existsByNameIgnoreCaseAndIdNot("Salted butter", 3L)).thenReturn(false);
        IngredientForm form = form(" Salted butter ");
        form.setShelfLifeDays(45);

        IngredientView updated = ingredientService.update(3L, form);

        assertThat(updated).isEqualTo(new IngredientView(3L, "Salted butter", MeasureUnit.GRAM, 45));
        assertThat(butter.getName()).isEqualTo("Salted butter");
        verify(fridgeItemRepository, never()).countByIngredientId(any());
    }

    @Test
    void updateRejectsNameOfAnotherIngredient() {
        when(ingredientRepository.findById(3L)).thenReturn(Optional.of(TestData.ingredient(3L, "Butter", MeasureUnit.GRAM)));
        when(ingredientRepository.existsByNameIgnoreCaseAndIdNot("Milk", 3L)).thenReturn(true);
        IngredientForm duplicate = form("Milk");

        assertThatThrownBy(() -> ingredientService.update(3L, duplicate))
                .isInstanceOf(DuplicateIngredientException.class)
                .hasMessage("Ingredient 'Milk' already exists");
    }

    @Test
    void updateChangesUnitOfUnusedIngredient() {
        Ingredient oil = TestData.ingredient(5L, "Olive oil", MeasureUnit.GRAM);
        when(ingredientRepository.findById(5L)).thenReturn(Optional.of(oil));
        stubUsage(5L, 0, 0, 0);
        IngredientForm form = form("Olive oil");
        form.setUnit(MeasureUnit.MILLILITER);

        ingredientService.update(5L, form);

        assertThat(oil.getUnit()).isEqualTo(MeasureUnit.MILLILITER);
    }

    @Test
    void updateRefusesToChangeUnitOfUsedIngredient() {
        Ingredient cheese = TestData.ingredient(4L, "Cheese", MeasureUnit.GRAM);
        when(ingredientRepository.findById(4L)).thenReturn(Optional.of(cheese));
        stubUsage(4L, 2, 1, 0);
        IngredientForm form = form("Cheese");
        form.setUnit(MeasureUnit.PIECE);

        assertThatThrownBy(() -> ingredientService.update(4L, form))
                .isInstanceOf(IngredientInUseException.class)
                .hasMessage("Cannot change the unit of Cheese: it is used in 2 recipes, 1 fridge item");
        assertThat(cheese.getUnit()).isEqualTo(MeasureUnit.GRAM);
    }

    @Test
    void deleteRemovesUnusedIngredient() {
        Ingredient honey = TestData.ingredient(9L, "Honey", MeasureUnit.GRAM);
        when(ingredientRepository.findById(9L)).thenReturn(Optional.of(honey));
        stubUsage(9L, 0, 0, 0);

        assertThat(ingredientService.delete(9L)).isEqualTo("Honey");
        verify(ingredientRepository).delete(honey);
    }

    @Test
    void deleteRefusesUsedIngredient() {
        when(ingredientRepository.findById(9L)).thenReturn(Optional.of(TestData.ingredient(9L, "Honey", MeasureUnit.GRAM)));
        stubUsage(9L, 0, 0, 1);

        assertThatThrownBy(() -> ingredientService.delete(9L))
                .isInstanceOf(IngredientInUseException.class)
                .hasMessage("Cannot delete Honey: it is used in 1 shopping list item");
        verify(ingredientRepository, never()).delete(any());
    }

    @Test
    void deleteThrowsWhenMissing() {
        when(ingredientRepository.findById(42L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ingredientService.delete(42L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    private void stubUsage(Long id, long recipes, long fridgeItems, long shoppingListItems) {
        when(recipeRepository.countUsingIngredient(id)).thenReturn(recipes);
        when(fridgeItemRepository.countByIngredientId(id)).thenReturn(fridgeItems);
        when(shoppingListItemRepository.countByIngredientId(id)).thenReturn(shoppingListItems);
    }

    private static IngredientCount count(Long ingredientId, long total) {
        return new IngredientCount() {
            @Override
            public Long getIngredientId() {
                return ingredientId;
            }

            @Override
            public long getTotal() {
                return total;
            }
        };
    }

    private static IngredientForm form(String name) {
        IngredientForm form = new IngredientForm();
        form.setName(name);
        form.setUnit(MeasureUnit.GRAM);
        form.setShelfLifeDays(30);
        return form;
    }
}
