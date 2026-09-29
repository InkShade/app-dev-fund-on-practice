package com.fridgechef.integration;

import static com.fridgechef.TestData.TODAY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

import com.fridgechef.domain.FridgeItem;
import com.fridgechef.domain.Ingredient;
import com.fridgechef.domain.MeasureUnit;
import com.fridgechef.domain.Recipe;

/** Demo function #1: find a recipe for what is in the fridge and cook it. */
class CookingFlowIT extends AbstractIntegrationTest {

    private Ingredient eggs;
    private Ingredient milk;
    private Long omeletteId;

    @BeforeEach
    void setUp() {
        eggs = saveIngredient("Eggs", MeasureUnit.PIECE, 21);
        milk = saveIngredient("Milk", MeasureUnit.MILLILITER, 7);
        omeletteId = recipeRepository.save(new Recipe("Omelette", "Quick breakfast", "Whisk and fry", 10)
                .addIngredient(eggs, 3)
                .addIngredient(milk, 300)).getId();
    }

    @Test
    void productsAddedThroughTheUiMakeTheRecipeCookable() throws Exception {
        addToFridge(eggs, "6", TODAY.plusDays(10).toString());

        mockMvc.perform(get("/recipes"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Omelette")))
                .andExpect(content().string(containsString("50%")));

        addToFridge(milk, "500", "");

        mockMvc.perform(get("/recipes/{id}", omeletteId))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("100% match")));
        assertThat(fridgeItemRepository.findAll())
                .extracting(FridgeItem::getExpiryDate)
                .contains(TODAY.plusDays(7));
    }

    @Test
    @Transactional
    void cookingTakesIngredientsOutOfTheFridgeExpiringFirst() throws Exception {
        fridgeItemRepository.save(new FridgeItem(eggs, 6, TODAY.plusDays(10)));
        fridgeItemRepository.save(new FridgeItem(milk, 200, TODAY.plusDays(1)));
        fridgeItemRepository.save(new FridgeItem(milk, 1000, TODAY.plusDays(6)));
        fridgeItemRepository.save(new FridgeItem(milk, 500, TODAY.minusDays(1)));

        mockMvc.perform(post("/recipes/{id}/cook", omeletteId))
                .andExpect(redirectedUrl("/recipes/" + omeletteId))
                .andExpect(flash().attribute("successMessage",
                        "Enjoy your Omelette! The ingredients were taken from the fridge."));

        assertThat(fridgeItemRepository.findAllByOrderByExpiryDateAscIdAsc())
                .extracting(item -> item.getIngredient().getName(), FridgeItem::getQuantity, FridgeItem::getExpiryDate)
                .containsExactly(
                        tuple("Milk", 500.0, TODAY.minusDays(1)),
                        tuple("Milk", 900.0, TODAY.plusDays(6)),
                        tuple("Eggs", 3.0, TODAY.plusDays(10)));
    }

    @Test
    void cookingIsRefusedWhenIngredientsAreMissing() throws Exception {
        fridgeItemRepository.save(new FridgeItem(eggs, 2, TODAY.plusDays(10)));

        mockMvc.perform(post("/recipes/{id}/cook", omeletteId))
                .andExpect(redirectedUrl("/recipes/" + omeletteId))
                .andExpect(flash().attribute("errorMessage", "Not enough ingredients: Eggs (1 pcs), Milk (300 ml)"));

        assertThat(fridgeItemRepository.findAll()).singleElement()
                .extracting(FridgeItem::getQuantity).isEqualTo(2.0);
    }

    @Test
    void expiredProductsCanBeThrownAway() throws Exception {
        fridgeItemRepository.save(new FridgeItem(milk, 500, TODAY.minusDays(1)));
        fridgeItemRepository.save(new FridgeItem(eggs, 6, TODAY));

        mockMvc.perform(get("/"))
                .andExpect(content().string(containsString("expires today")));
        mockMvc.perform(post("/fridge/discard-expired"))
                .andExpect(flash().attribute("successMessage", "Threw away 1 expired product(s)"));

        assertThat(fridgeItemRepository.findAll()).extracting(item -> item.getIngredient().getId())
                .containsExactly(eggs.getId());
    }

    private void addToFridge(Ingredient ingredient, String quantity, String expiryDate) throws Exception {
        mockMvc.perform(post("/fridge")
                        .param("ingredientId", ingredient.getId().toString())
                        .param("quantity", quantity)
                        .param("expiryDate", expiryDate))
                .andExpect(redirectedUrl("/fridge"));
    }
}
