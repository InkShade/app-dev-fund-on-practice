package com.fridgechef.mapper;

import org.springframework.stereotype.Component;

import com.fridgechef.domain.FridgeItem;
import com.fridgechef.domain.Ingredient;
import com.fridgechef.dto.FridgeItemView;
import com.fridgechef.service.FreshnessPolicy;

@Component
public class FridgeItemMapper {

    private final FreshnessPolicy freshnessPolicy;

    public FridgeItemMapper(FreshnessPolicy freshnessPolicy) {
        this.freshnessPolicy = freshnessPolicy;
    }

    public FridgeItemView toView(FridgeItem item) {
        Ingredient ingredient = item.getIngredient();
        return new FridgeItemView(
                item.getId(),
                ingredient.getName(),
                item.getQuantity(),
                ingredient.getUnit(),
                item.getExpiryDate(),
                freshnessPolicy.daysLeft(item.getExpiryDate()),
                freshnessPolicy.statusOf(item.getExpiryDate()));
    }
}
