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
import com.fridgechef.domain.ShoppingListItem;

/** Demo function #2: put what a recipe lacks on the shopping list, buy it and stock the fridge. */
class ShoppingFlowIT extends AbstractIntegrationTest {

    private Ingredient flour;
    private Ingredient milk;
    private Long pancakesId;

    @BeforeEach
    void setUp() {
        flour = saveIngredient("Flour", MeasureUnit.GRAM, 180);
        milk = saveIngredient("Milk", MeasureUnit.MILLILITER, 7);
        pancakesId = recipeRepository.save(new Recipe("Pancakes", "Weekend breakfast", "Mix and fry", 30)
                .addIngredient(flour, 200)
                .addIngredient(milk, 300)).getId();
        fridgeItemRepository.save(new FridgeItem(milk, 100, TODAY.plusDays(5)));
    }

    @Test
    void missingIngredientsGoToShoppingListAndBackIntoTheFridge() throws Exception {
        mockMvc.perform(post("/recipes/{id}/shopping-list", pancakesId))
                .andExpect(flash().attribute("successMessage", "2 missing ingredient(s) added to the shopping list"));

        mockMvc.perform(get("/shopping-list"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("200 g")))
                .andExpect(content().string(containsString("200 ml")));

        for (ShoppingListItem item : shoppingListItemRepository.findAll()) {
            mockMvc.perform(post("/shopping-list/{id}/toggle", item.getId()))
                    .andExpect(redirectedUrl("/shopping-list"));
        }
        mockMvc.perform(post("/shopping-list/move-to-fridge"))
                .andExpect(flash().attribute("successMessage", "2 product(s) moved into the fridge"));

        assertThat(shoppingListItemRepository.count()).isZero();
        mockMvc.perform(get("/recipes/{id}", pancakesId))
                .andExpect(content().string(containsString("100% match")));
        assertThat(fridgeItemRepository.findAll())
                .filteredOn(item -> item.getIngredient().getId().equals(flour.getId()))
                .singleElement()
                .satisfies(item -> {
                    assertThat(item.getQuantity()).isEqualTo(200);
                    assertThat(item.getExpiryDate()).isEqualTo(TODAY.plusDays(180));
                });
    }

    @Test
    void addingTheSameIngredientTwiceMergesQuantities() throws Exception {
        addToShoppingList(milk, "500");
        addToShoppingList(milk, "250");

        assertThat(shoppingListItemRepository.findAll()).singleElement()
                .extracting(ShoppingListItem::getQuantity).isEqualTo(750.0);
    }

    @Test
    void itemCanBeRemoved() throws Exception {
        addToShoppingList(flour, "1000");
        Long id = shoppingListItemRepository.findAll().getFirst().getId();

        mockMvc.perform(post("/shopping-list/{id}/delete", id)).andExpect(redirectedUrl("/shopping-list"));

        assertThat(shoppingListItemRepository.count()).isZero();
    }

    @Test
    void newIngredientCanBeAddedToCatalogueButNotTwice() throws Exception {
        mockMvc.perform(post("/ingredients").param("name", "Honey").param("unit", "GRAM").param("shelfLifeDays", "365"))
                .andExpect(redirectedUrl("/ingredients"));
        mockMvc.perform(post("/ingredients").param("name", "honey").param("unit", "GRAM").param("shelfLifeDays", "365"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("already exists")));

        assertThat(ingredientRepository.findAll()).extracting(Ingredient::getName).containsOnlyOnce("Honey");
    }

    @Test
    void unknownShoppingListItemShowsNotFoundPage() throws Exception {
        mockMvc.perform(post("/shopping-list/{id}/toggle", 12345))
                .andExpect(status().isNotFound())
                .andExpect(content().string(containsString("Shopping list item with id 12345 was not found")));
    }

    private void addToShoppingList(Ingredient ingredient, String quantity) throws Exception {
        mockMvc.perform(post("/shopping-list")
                        .param("ingredientId", ingredient.getId().toString())
                        .param("quantity", quantity))
                .andExpect(redirectedUrl("/shopping-list"));
    }
}
