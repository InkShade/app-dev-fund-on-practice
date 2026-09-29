package com.fridgechef.service;

import static com.fridgechef.TestData.TODAY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fridgechef.TestData;
import com.fridgechef.domain.FridgeItem;
import com.fridgechef.domain.Ingredient;
import com.fridgechef.domain.MeasureUnit;
import com.fridgechef.dto.IngredientRequirement;
import com.fridgechef.dto.RecipeMatch;
import com.fridgechef.exception.InsufficientIngredientsException;
import com.fridgechef.repository.FridgeItemRepository;

@ExtendWith(MockitoExtension.class)
class CookingServiceTest {

    @Mock
    private RecipeMatchingService recipeMatchingService;

    @Mock
    private FridgeItemRepository fridgeItemRepository;

    private CookingService cookingService;

    private final Ingredient milk = TestData.ingredient(2L, "Milk", MeasureUnit.MILLILITER);

    @BeforeEach
    void setUp() {
        cookingService = new CookingService(recipeMatchingService, fridgeItemRepository, TestData.freshnessPolicy());
    }

    @Test
    void cookingUsesProductsThatExpireFirst() {
        givenMatch(requirement(300, 700));
        FridgeItem expiresTomorrow = TestData.fridgeItem(1L, milk, 200, TODAY.plusDays(1));
        FridgeItem expiresNextWeek = TestData.fridgeItem(2L, milk, 500, TODAY.plusDays(7));
        givenUsableMilk(expiresTomorrow, expiresNextWeek);

        RecipeMatch cooked = cookingService.cook(1L);

        assertThat(cooked.name()).isEqualTo("Porridge");
        assertThat(expiresTomorrow.getQuantity()).isZero();
        assertThat(expiresNextWeek.getQuantity()).isEqualTo(400);
        verify(fridgeItemRepository).delete(expiresTomorrow);
        verify(fridgeItemRepository, never()).delete(expiresNextWeek);
    }

    @Test
    void laterItemsAreNotTouchedOnceTheRequiredAmountIsTaken() {
        givenMatch(requirement(100, 700));
        FridgeItem first = TestData.fridgeItem(1L, milk, 200, TODAY.plusDays(1));
        FridgeItem second = TestData.fridgeItem(2L, milk, 500, TODAY.plusDays(7));
        givenUsableMilk(first, second);

        cookingService.cook(1L);

        assertThat(first.getQuantity()).isEqualTo(100);
        assertThat(second.getQuantity()).isEqualTo(500);
        verify(fridgeItemRepository, never()).delete(any());
    }

    @Test
    void cookingFailsWhenIngredientsAreMissing() {
        givenMatch(requirement(300, 120));

        assertThatThrownBy(() -> cookingService.cook(1L))
                .isInstanceOfSatisfying(InsufficientIngredientsException.class, ex ->
                        assertThat(ex.getMissingIngredients()).containsExactly("Milk (180 ml)"))
                .hasMessage("Not enough ingredients to cook Porridge: Milk (180 ml)");
        verifyNoInteractions(fridgeItemRepository);
    }

    private void givenMatch(IngredientRequirement requirement) {
        List<IngredientRequirement> requirements = List.of(requirement);
        int percent = RecipeMatchingService.matchPercent(requirements);
        when(recipeMatchingService.getMatch(1L))
                .thenReturn(new RecipeMatch(1L, "Porridge", "desc", "steps", 15, percent, 0, requirements));
    }

    private void givenUsableMilk(FridgeItem... items) {
        when(fridgeItemRepository.findByIngredientIdAndExpiryDateGreaterThanEqualOrderByExpiryDateAscIdAsc(2L, TODAY))
                .thenReturn(List.of(items));
    }

    private IngredientRequirement requirement(double required, double available) {
        return new IngredientRequirement(milk.getId(), milk.getName(), milk.getUnit(), required, available, false);
    }
}
