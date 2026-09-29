package com.fridgechef.dto;

import com.fridgechef.domain.MeasureUnit;

public record ShoppingListItemView(Long id, String ingredientName, double quantity, MeasureUnit unit, boolean purchased) {

    public String quantityLabel() {
        return Quantities.format(quantity, unit);
    }
}
