package com.fridgechef.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public class ShoppingItemForm {

    @NotNull(message = "Choose an ingredient")
    private Long ingredientId;

    @NotNull(message = "Quantity is required")
    @Positive(message = "Quantity must be greater than zero")
    private Double quantity;

    public Long getIngredientId() {
        return ingredientId;
    }

    public void setIngredientId(Long ingredientId) {
        this.ingredientId = ingredientId;
    }

    public Double getQuantity() {
        return quantity;
    }

    public void setQuantity(Double quantity) {
        this.quantity = quantity;
    }
}
