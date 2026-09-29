package com.fridgechef.dto;

import java.util.ArrayList;
import java.util.List;

/** Where an ingredient is referenced. A used ingredient cannot be deleted or change its unit. */
public record IngredientUsage(long recipes, long fridgeItems, long shoppingListItems) {

    public boolean isUsed() {
        return recipes + fridgeItems + shoppingListItems > 0;
    }

    /** For example "2 recipes, 1 fridge item". */
    public String summary() {
        List<String> parts = new ArrayList<>();
        addPart(parts, recipes, "recipe");
        addPart(parts, fridgeItems, "fridge item");
        addPart(parts, shoppingListItems, "shopping list item");
        return parts.isEmpty() ? "nothing" : String.join(", ", parts);
    }

    private static void addPart(List<String> parts, long count, String noun) {
        if (count > 0) {
            parts.add(count + " " + noun + (count == 1 ? "" : "s"));
        }
    }
}
