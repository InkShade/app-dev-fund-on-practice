package com.fridgechef.mapper;

import org.springframework.stereotype.Component;

import com.fridgechef.domain.Ingredient;
import com.fridgechef.dto.IngredientForm;
import com.fridgechef.dto.IngredientView;

@Component
public class IngredientMapper {

    public IngredientView toView(Ingredient ingredient) {
        return new IngredientView(
                ingredient.getId(), ingredient.getName(), ingredient.getUnit(), ingredient.getShelfLifeDays());
    }

    public Ingredient toEntity(IngredientForm form) {
        return new Ingredient(form.getName().strip(), form.getUnit(), form.getShelfLifeDays());
    }
}
