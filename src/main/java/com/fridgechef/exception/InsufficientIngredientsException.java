package com.fridgechef.exception;

import java.util.List;

public class InsufficientIngredientsException extends RuntimeException {

    private final List<String> missingIngredients;

    public InsufficientIngredientsException(String recipeName, List<String> missingIngredients) {
        super("Not enough ingredients to cook " + recipeName + ": " + String.join(", ", missingIngredients));
        this.missingIngredients = List.copyOf(missingIngredients);
    }

    public List<String> getMissingIngredients() {
        return missingIngredients;
    }
}
