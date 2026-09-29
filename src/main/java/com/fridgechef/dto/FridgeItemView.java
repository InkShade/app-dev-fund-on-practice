package com.fridgechef.dto;

import java.time.LocalDate;

import com.fridgechef.domain.FreshnessStatus;
import com.fridgechef.domain.MeasureUnit;

public record FridgeItemView(
        Long id,
        String ingredientName,
        double quantity,
        MeasureUnit unit,
        LocalDate expiryDate,
        long daysLeft,
        FreshnessStatus status) {

    public String quantityLabel() {
        return Quantities.format(quantity, unit);
    }
}
