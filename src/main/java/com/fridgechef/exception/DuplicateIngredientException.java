package com.fridgechef.exception;

public class DuplicateIngredientException extends RuntimeException {

    public DuplicateIngredientException(String name) {
        super("Ingredient '" + name + "' already exists");
    }
}
