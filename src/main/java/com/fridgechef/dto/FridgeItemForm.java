package com.fridgechef.dto;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public class FridgeItemForm {

    @NotNull(message = "Choose an ingredient")
    private Long ingredientId;

    @NotNull(message = "Quantity is required")
    @Positive(message = "Quantity must be greater than zero")
    private Double quantity;

    /** Optional: when empty, the ingredient's usual shelf life is used. */
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate expiryDate;

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

    public LocalDate getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(LocalDate expiryDate) {
        this.expiryDate = expiryDate;
    }
}
