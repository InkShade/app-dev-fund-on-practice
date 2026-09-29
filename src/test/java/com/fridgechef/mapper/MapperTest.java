package com.fridgechef.mapper;

import static com.fridgechef.TestData.TODAY;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.fridgechef.TestData;
import com.fridgechef.domain.FreshnessStatus;
import com.fridgechef.domain.Ingredient;
import com.fridgechef.domain.MeasureUnit;
import com.fridgechef.dto.FridgeItemView;
import com.fridgechef.dto.IngredientForm;
import com.fridgechef.dto.IngredientView;
import com.fridgechef.dto.ShoppingListItemView;

class MapperTest {

    private final Ingredient milk = TestData.ingredient(1L, "Milk", MeasureUnit.MILLILITER, 7);

    @Test
    void mapsIngredientToViewAndFormToEntity() {
        assertThat(new IngredientMapper().toView(milk))
                .isEqualTo(new IngredientView(1L, "Milk", MeasureUnit.MILLILITER, 7));

        IngredientForm form = new IngredientForm();
        form.setName("  Butter ");
        form.setUnit(MeasureUnit.GRAM);
        form.setShelfLifeDays(30);
        Ingredient butter = new IngredientMapper().toEntity(form);

        assertThat(butter.getName()).isEqualTo("Butter");
        assertThat(butter.getUnit()).isEqualTo(MeasureUnit.GRAM);
        assertThat(butter.getShelfLifeDays()).isEqualTo(30);
    }

    @Test
    void mapsFridgeItemWithFreshness() {
        FridgeItemMapper mapper = new FridgeItemMapper(TestData.freshnessPolicy());

        FridgeItemView view = mapper.toView(TestData.fridgeItem(5L, milk, 750, TODAY.plusDays(2)));

        assertThat(view).isEqualTo(new FridgeItemView(
                5L, "Milk", 750, MeasureUnit.MILLILITER, TODAY.plusDays(2), 2, FreshnessStatus.EXPIRING_SOON));
        assertThat(view.quantityLabel()).isEqualTo("750 ml");
    }

    @Test
    void mapsShoppingListItem() {
        ShoppingListItemView view = new ShoppingListItemMapper().toView(TestData.shoppingItem(3L, milk, 1000));

        assertThat(view).isEqualTo(new ShoppingListItemView(3L, "Milk", 1000, MeasureUnit.MILLILITER, false));
    }
}
