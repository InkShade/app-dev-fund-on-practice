package com.fridgechef.dto;

import java.util.List;

/**
 * A recipe scored against the current fridge contents.
 *
 * @param matchPercent          share of the recipe's ingredients available in the needed quantity
 * @param expiringIngredientsUsed how many of the recipe's ingredients would use up products that expire soon
 */
public record RecipeMatch(
        Long recipeId,
        String name,
        String description,
        String instructions,
        int cookingTimeMinutes,
        int matchPercent,
        int expiringIngredientsUsed,
        List<IngredientRequirement> requirements) {

    public RecipeMatch {
        requirements = List.copyOf(requirements);
    }

    public boolean canCook() {
        return requirements.stream().allMatch(IngredientRequirement::isSatisfied);
    }

    public List<IngredientRequirement> missingIngredients() {
        return requirements.stream()
                .filter(requirement -> !requirement.isSatisfied())
                .toList();
    }
}
