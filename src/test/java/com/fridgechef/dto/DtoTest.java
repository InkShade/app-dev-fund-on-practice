package com.fridgechef.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import com.fridgechef.domain.FreshnessStatus;
import com.fridgechef.domain.MeasureUnit;

class DtoTest {

    private final IngredientRequirement eggs =
            new IngredientRequirement(1L, "Eggs", MeasureUnit.PIECE, 3, 6, false);
    private final IngredientRequirement milk =
            new IngredientRequirement(2L, "Milk", MeasureUnit.MILLILITER, 200, 50, true);

    @Test
    void requirementReportsMissingQuantity() {
        assertThat(eggs.isSatisfied()).isTrue();
        assertThat(eggs.missingQuantity()).isZero();
        assertThat(milk.isSatisfied()).isFalse();
        assertThat(milk.missingQuantity()).isEqualTo(150);
        assertThat(milk.requiredLabel()).isEqualTo("200 ml");
        assertThat(milk.availableLabel()).isEqualTo("50 ml");
        assertThat(milk.missingLabel()).isEqualTo("150 ml");
    }

    @Test
    void recipeCanBeCookedOnlyWhenEveryIngredientIsAvailable() {
        RecipeMatch partial = match(List.of(eggs, milk));
        RecipeMatch full = match(List.of(eggs));

        assertThat(partial.canCook()).isFalse();
        assertThat(partial.missingIngredients()).containsExactly(milk);
        assertThat(full.canCook()).isTrue();
        assertThat(full.missingIngredients()).isEmpty();
    }

    @Test
    void requirementsAreDefensivelyCopied() {
        List<IngredientRequirement> requirements = new ArrayList<>(List.of(eggs));
        RecipeMatch match = match(requirements);

        requirements.add(milk);

        assertThat(match.requirements()).containsExactly(eggs);
    }

    @Test
    void viewsFormatTheirQuantities() {
        assertThat(new ShoppingListItemView(1L, "Flour", 1000, MeasureUnit.GRAM, false).quantityLabel())
                .isEqualTo("1000 g");
    }

    @ParameterizedTest
    @CsvSource({
            "0, expires today",
            "1, 1 day left",
            "5, 5 days left",
            "-1, expired 1 day ago",
            "-4, expired 4 days ago"
    })
    void fridgeItemDescribesExpiry(long daysLeft, String expected) {
        FridgeItemView view = new FridgeItemView(1L, "Milk", 1, MeasureUnit.MILLILITER,
                LocalDate.now(), daysLeft, FreshnessStatus.FRESH);

        assertThat(view.expiryLabel()).isEqualTo(expected);
    }

    private static RecipeMatch match(List<IngredientRequirement> requirements) {
        return new RecipeMatch(1L, "Omelette", "desc", "steps", 10, 50, 1, requirements);
    }
}
