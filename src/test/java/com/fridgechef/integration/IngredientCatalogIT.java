package com.fridgechef.integration;

import static com.fridgechef.TestData.TODAY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fridgechef.domain.FridgeItem;
import com.fridgechef.domain.Ingredient;
import com.fridgechef.domain.MeasureUnit;
import com.fridgechef.domain.Recipe;

/** Editing and deleting catalogue ingredients must keep the fridge, recipes and shopping list consistent. */
class IngredientCatalogIT extends AbstractIntegrationTest {

    private Ingredient milk;
    private Ingredient honey;
    private Long hotMilkId;

    @BeforeEach
    void setUp() {
        milk = saveIngredient("Milk", MeasureUnit.MILLILITER, 7);
        honey = saveIngredient("Honey", MeasureUnit.GRAM, 365);
        fridgeItemRepository.save(new FridgeItem(milk, 1000, TODAY.plusDays(2)));
        hotMilkId = recipeRepository.save(new Recipe("Hot milk", "Warming drink", "Heat the milk", 5).addIngredient(milk, 250)).getId();
    }

    @Test
    void renamedIngredientShowsUpInFridgeAndRecipes() throws Exception {
        mockMvc.perform(post("/ingredients/{id}", milk.getId())
                        .param("name", "Oat milk")
                        .param("unit", "MILLILITER")
                        .param("shelfLifeDays", "10"))
                .andExpect(redirectedUrl("/ingredients"))
                .andExpect(flash().attribute("successMessage", "Oat milk was updated"));

        mockMvc.perform(get("/fridge")).andExpect(content().string(containsString("Oat milk")));
        mockMvc.perform(get("/recipes/{id}", hotMilkId))
                .andExpect(content().string(containsString("Oat milk")))
                .andExpect(content().string(containsString("100% match")));
        assertThat(fridgeItemRepository.findAll()).singleElement()
                .extracting(FridgeItem::getExpiryDate)
                .as("existing best-before dates are not recalculated")
                .isEqualTo(TODAY.plusDays(2));
    }

    @Test
    void unitOfUsedIngredientCannotBeChanged() throws Exception {
        mockMvc.perform(post("/ingredients/{id}", milk.getId())
                        .param("name", "Milk")
                        .param("unit", "GRAM")
                        .param("shelfLifeDays", "7"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(
                        "Cannot change the unit of Milk: it is used in 1 recipe, 1 fridge item")));

        assertThat(ingredientRepository.findById(milk.getId()).orElseThrow().getUnit())
                .isEqualTo(MeasureUnit.MILLILITER);
    }

    @Test
    void usedIngredientCannotBeDeletedButUnusedCan() throws Exception {
        mockMvc.perform(post("/ingredients/{id}/delete", milk.getId()))
                .andExpect(flash().attribute("errorMessage", "Cannot delete Milk: it is used in 1 recipe, 1 fridge item"));
        mockMvc.perform(post("/ingredients/{id}/delete", honey.getId()))
                .andExpect(flash().attribute("successMessage", "Honey was removed from the catalogue"));

        assertThat(ingredientRepository.findAll()).extracting(Ingredient::getName).containsExactly("Milk");
    }

    @Test
    void catalogueShowsWhereIngredientsAreUsed() throws Exception {
        mockMvc.perform(get("/ingredients"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("1 recipe, 1 fridge item")))
                .andExpect(content().string(containsString("not used")));
        mockMvc.perform(get("/ingredients/{id}/edit", milk.getId()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Edit Milk")));
    }
}
