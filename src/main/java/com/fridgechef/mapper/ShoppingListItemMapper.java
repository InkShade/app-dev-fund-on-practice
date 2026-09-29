package com.fridgechef.mapper;

import org.springframework.stereotype.Component;

import com.fridgechef.domain.Ingredient;
import com.fridgechef.domain.ShoppingListItem;
import com.fridgechef.dto.ShoppingListItemView;

@Component
public class ShoppingListItemMapper {

    public ShoppingListItemView toView(ShoppingListItem item) {
        Ingredient ingredient = item.getIngredient();
        return new ShoppingListItemView(
                item.getId(), ingredient.getName(), item.getQuantity(), ingredient.getUnit(), item.isPurchased());
    }
}
