package com.fridgechef.exception;

public class IngredientInUseException extends RuntimeException {

    public IngredientInUseException(String action, String ingredientName, String usage) {
        super("Cannot " + action + " " + ingredientName + ": it is used in " + usage);
    }
}
