package com.fridgechef.dto;

import com.fridgechef.domain.MeasureUnit;

/**
 * How much of an ingredient a recipe needs compared to what is in the fridge.
 *
 * @param expiringSoon whether some of this ingredient in the fridge is about to expire
 */
public record IngredientRequirement(
        Long ingredientId,
        String ingredientName,
        MeasureUnit unit,
        double required,
        double available,
        boolean expiringSoon) {

    public boolean isSatisfied() {
        return available >= required;
    }

    public double missingQuantity() {
        return Math.max(0, required - available);
    }

    public String requiredLabel() {
        return Quantities.format(required, unit);
    }

    public String availableLabel() {
        return Quantities.format(available, unit);
    }

    public String missingLabel() {
        return Quantities.format(missingQuantity(), unit);
    }
}
