package com.fridgechef.service;

import static com.fridgechef.TestData.TODAY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fridgechef.TestData;
import com.fridgechef.domain.FridgeItem;
import com.fridgechef.domain.Ingredient;
import com.fridgechef.domain.MeasureUnit;
import com.fridgechef.domain.Recipe;
import com.fridgechef.dto.IngredientRequirement;
import com.fridgechef.dto.RecipeMatch;
import com.fridgechef.exception.ResourceNotFoundException;
import com.fridgechef.repository.FridgeItemRepository;
import com.fridgechef.repository.RecipeRepository;

@ExtendWith(MockitoExtension.class)
class RecipeMatchingServiceTest {

    @Mock
    private RecipeRepository recipeRepository;

    @Mock
    private FridgeItemRepository fridgeItemRepository;

    private RecipeMatchingService service;

    private final Ingredient eggs = TestData.ingredient(1L, "Eggs", MeasureUnit.PIECE);
    private final Ingredient milk = TestData.ingredient(2L, "Milk", MeasureUnit.MILLILITER);
    private final Ingredient flour = TestData.ingredient(3L, "Flour", MeasureUnit.GRAM);
    private final Ingredient cheese = TestData.ingredient(4L, "Cheese", MeasureUnit.GRAM);

    @BeforeEach
    void setUp() {
        service = new RecipeMatchingService(recipeRepository, fridgeItemRepository, TestData.freshnessPolicy());
    }

    @Test
    void recipeWithAllIngredientsInStockMatchesFully() {
        givenFridge(
                TestData.fridgeItem(1L, eggs, 6, TODAY.plusDays(10)),
                TestData.fridgeItem(2L, milk, 500, TODAY.plusDays(10)));
        givenRecipes(omelette());

        RecipeMatch match = service.findMatches(0).getFirst();

        assertThat(match.matchPercent()).isEqualTo(100);
        assertThat(match.canCook()).isTrue();
        assertThat(match.missingIngredients()).isEmpty();
    }

    @Test
    void quantitiesOfSeveralItemsOfTheSameIngredientAreAddedUp() {
        givenFridge(
                TestData.fridgeItem(1L, eggs, 2, TODAY.plusDays(10)),
                TestData.fridgeItem(2L, eggs, 2, TODAY.plusDays(12)),
                TestData.fridgeItem(3L, milk, 100, TODAY.plusDays(10)));
        givenRecipes(omelette());

        IngredientRequirement eggsRequirement = service.findMatches(0).getFirst().requirements().getFirst();

        assertThat(eggsRequirement.available()).isEqualTo(4);
        assertThat(eggsRequirement.isSatisfied()).isTrue();
    }

    @Test
    void notEnoughQuantityCountsAsMissing() {
        givenFridge(
                TestData.fridgeItem(1L, eggs, 1, TODAY.plusDays(10)),
                TestData.fridgeItem(2L, milk, 500, TODAY.plusDays(10)));
        givenRecipes(omelette());

        RecipeMatch match = service.findMatches(0).getFirst();

        assertThat(match.matchPercent()).isEqualTo(50);
        assertThat(match.missingIngredients())
                .singleElement()
                .satisfies(missing -> {
                    assertThat(missing.ingredientName()).isEqualTo("Eggs");
                    assertThat(missing.missingQuantity()).isEqualTo(2);
                });
    }

    @Test
    void expiredProductsAreNotCountedAsAvailable() {
        givenFridge(
                TestData.fridgeItem(1L, eggs, 6, TODAY.minusDays(1)),
                TestData.fridgeItem(2L, milk, 500, TODAY.plusDays(10)));
        givenRecipes(omelette());

        RecipeMatch match = service.findMatches(0).getFirst();

        assertThat(match.requirements().getFirst().available()).isZero();
        assertThat(match.matchPercent()).isEqualTo(50);
    }

    @Test
    void recipesAreSortedByMatchThenByExpiringProductsThenByName() {
        givenFridge(
                TestData.fridgeItem(1L, eggs, 12, TODAY.plusDays(10)),
                TestData.fridgeItem(2L, milk, 1000, TODAY.plusDays(1)),
                TestData.fridgeItem(3L, flour, 1000, TODAY.plusDays(60)));
        Recipe pancakes = TestData.recipe(2L, "Pancakes").addIngredient(eggs, 2).addIngredient(milk, 300)
                .addIngredient(flour, 200);
        Recipe boiledEggs = TestData.recipe(3L, "Boiled eggs").addIngredient(eggs, 4);
        Recipe cheeseOmelette = TestData.recipe(4L, "Cheese omelette").addIngredient(eggs, 3)
                .addIngredient(cheese, 50);
        Recipe bread = TestData.recipe(5L, "Bread").addIngredient(flour, 500);
        givenRecipes(bread, boiledEggs, cheeseOmelette, omelette(), pancakes);

        assertThat(service.findMatches(0))
                .extracting(RecipeMatch::name, RecipeMatch::matchPercent)
                .containsExactly(
                        tuple("Omelette", 100),
                        tuple("Pancakes", 100),
                        tuple("Boiled eggs", 100),
                        tuple("Bread", 100),
                        tuple("Cheese omelette", 50));
    }

    @Test
    void expiringProductsUsedByRecipeAreCounted() {
        givenFridge(
                TestData.fridgeItem(1L, eggs, 6, TODAY.plusDays(2)),
                TestData.fridgeItem(2L, milk, 500, TODAY));
        givenRecipes(omelette());

        RecipeMatch match = service.findMatches(0).getFirst();

        assertThat(match.expiringIngredientsUsed()).isEqualTo(2);
        assertThat(match.requirements()).allMatch(IngredientRequirement::expiringSoon);
    }

    @Test
    void recipesBelowMinimumMatchAreFilteredOut() {
        givenFridge(TestData.fridgeItem(1L, eggs, 6, TODAY.plusDays(10)));
        givenRecipes(omelette(), TestData.recipe(9L, "Cheese plate").addIngredient(cheese, 200));

        assertThat(service.findMatches(50)).extracting(RecipeMatch::name).containsExactly("Omelette");
        assertThat(service.findMatches(0)).hasSize(2);
    }

    @Test
    void getMatchScoresSingleRecipe() {
        givenFridge(TestData.fridgeItem(1L, eggs, 6, TODAY.plusDays(10)));
        when(recipeRepository.findWithIngredientsById(1L)).thenReturn(Optional.of(omelette()));

        RecipeMatch match = service.getMatch(1L);

        assertThat(match.recipeId()).isEqualTo(1L);
        assertThat(match.instructions()).isEqualTo("Cook Omelette");
        assertThat(match.cookingTimeMinutes()).isEqualTo(20);
        assertThat(match.matchPercent()).isEqualTo(50);
    }

    @Test
    void getMatchThrowsForUnknownRecipe() {
        when(recipeRepository.findWithIngredientsById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getMatch(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Recipe with id 99 was not found");
    }

    @Test
    void matchPercentIsRoundedDownAndEmptyRecipeIsAlwaysCookable() {
        IngredientRequirement ok = new IngredientRequirement(1L, "A", MeasureUnit.GRAM, 1, 1, false);
        IngredientRequirement missing = new IngredientRequirement(2L, "B", MeasureUnit.GRAM, 1, 0, false);

        assertThat(RecipeMatchingService.matchPercent(List.of(ok, ok, missing))).isEqualTo(66);
        assertThat(RecipeMatchingService.matchPercent(List.of())).isEqualTo(100);
    }

    private Recipe omelette() {
        return TestData.recipe(1L, "Omelette").addIngredient(eggs, 3).addIngredient(milk, 100);
    }

    private void givenFridge(FridgeItem... items) {
        when(fridgeItemRepository.findAllByOrderByExpiryDateAscIdAsc()).thenReturn(List.of(items));
    }

    private void givenRecipes(Recipe... recipes) {
        when(recipeRepository.findAllByOrderByNameAsc()).thenReturn(List.of(recipes));
    }
}
