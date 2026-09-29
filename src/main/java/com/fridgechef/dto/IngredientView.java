package com.fridgechef.dto;

import com.fridgechef.domain.MeasureUnit;

public record IngredientView(Long id, String name, MeasureUnit unit, int shelfLifeDays) {
}
