package com.fridgechef.dto;

import com.fridgechef.domain.MeasureUnit;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class IngredientForm {

    @NotBlank(message = "Name is required")
    @Size(max = 100, message = "Name must be at most 100 characters")
    private String name;

    @NotNull(message = "Unit is required")
    private MeasureUnit unit;

    @Min(value = 1, message = "Shelf life must be at least 1 day")
    @Max(value = 3650, message = "Shelf life must be at most 3650 days")
    private int shelfLifeDays = 7;

    /** Pre-filled form for editing an existing ingredient. */
    public static IngredientForm from(IngredientView ingredient) {
        IngredientForm form = new IngredientForm();
        form.setName(ingredient.name());
        form.setUnit(ingredient.unit());
        form.setShelfLifeDays(ingredient.shelfLifeDays());
        return form;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public MeasureUnit getUnit() {
        return unit;
    }

    public void setUnit(MeasureUnit unit) {
        this.unit = unit;
    }

    public int getShelfLifeDays() {
        return shelfLifeDays;
    }

    public void setShelfLifeDays(int shelfLifeDays) {
        this.shelfLifeDays = shelfLifeDays;
    }
}
