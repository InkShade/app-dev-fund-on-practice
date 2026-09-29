package com.fridgechef.dto;

/** Catalogue entry: the ingredient together with where it is used. */
public record IngredientOverview(IngredientView ingredient, IngredientUsage usage) {
}
