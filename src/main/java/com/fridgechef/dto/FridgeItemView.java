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

    /** Human friendly expiry, e.g. "expires today", "3 days left", "expired 2 days ago". */
    public String expiryLabel() {
        if (daysLeft == 0) {
            return "expires today";
        }
        if (daysLeft > 0) {
            return daysLeft + (daysLeft == 1 ? " day left" : " days left");
        }
        long daysAgo = -daysLeft;
        return "expired " + daysAgo + (daysAgo == 1 ? " day ago" : " days ago");
    }
}
