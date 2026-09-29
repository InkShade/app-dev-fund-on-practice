package com.fridgechef.web;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fridgechef.domain.MeasureUnit;
import com.fridgechef.dto.IngredientRequirement;
import com.fridgechef.dto.RecipeMatch;
import com.fridgechef.exception.InsufficientIngredientsException;
import com.fridgechef.exception.ResourceNotFoundException;
import com.fridgechef.service.CookingService;
import com.fridgechef.service.RecipeMatchingService;
import com.fridgechef.service.ShoppingListService;

@WebMvcTest(RecipeController.class)
class RecipeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RecipeMatchingService recipeMatchingService;

    @MockitoBean
    private CookingService cookingService;

    @MockitoBean
    private ShoppingListService shoppingListService;

    private final RecipeMatch pasta = new RecipeMatch(3L, "Tomato pasta", "Simple pasta", "Boil and mix", 25, 50, 1,
            List.of(
                    new IngredientRequirement(1L, "Pasta", MeasureUnit.GRAM, 200, 0, false),
                    new IngredientRequirement(2L, "Tomatoes", MeasureUnit.PIECE, 3, 4, true)));

    @Test
    void listShowsMatchesAboveThreshold() throws Exception {
        when(recipeMatchingService.findMatches(50)).thenReturn(List.of(pasta));

        mockMvc.perform(get("/recipes").param("minMatch", "50"))
                .andExpect(status().isOk())
                .andExpect(view().name("recipes"))
                .andExpect(model().attribute("minMatch", 50))
                .andExpect(content().string(containsString("Tomato pasta")))
                .andExpect(content().string(containsString("Missing:")));
    }

    @Test
    void thresholdIsClampedToValidRange() throws Exception {
        when(recipeMatchingService.findMatches(100)).thenReturn(List.of());

        mockMvc.perform(get("/recipes").param("minMatch", "250"))
                .andExpect(model().attribute("minMatch", 100))
                .andExpect(content().string(containsString("No recipes match this filter")));
    }

    @Test
    void detailsShowWhatIsMissing() throws Exception {
        when(recipeMatchingService.getMatch(3L)).thenReturn(pasta);

        mockMvc.perform(get("/recipes/3"))
                .andExpect(status().isOk())
                .andExpect(view().name("recipe-details"))
                .andExpect(content().string(containsString("need 200 g")))
                .andExpect(content().string(containsString("Add missing to shopping list")));
    }

    @Test
    void unknownRecipeShowsNotFoundPage() throws Exception {
        when(recipeMatchingService.getMatch(99L)).thenThrow(new ResourceNotFoundException("Recipe", 99L));

        mockMvc.perform(get("/recipes/99"))
                .andExpect(status().isNotFound())
                .andExpect(view().name("error/not-found"));
    }

    @Test
    void cookingSuccessRedirectsWithMessage() throws Exception {
        when(cookingService.cook(3L)).thenReturn(pasta);

        mockMvc.perform(post("/recipes/3/cook"))
                .andExpect(redirectedUrl("/recipes/3"))
                .andExpect(flash().attribute(FlashMessages.SUCCESS_ATTRIBUTE,
                        "Enjoy your Tomato pasta! The ingredients were taken from the fridge."));
    }

    @Test
    void cookingWithoutIngredientsShowsError() throws Exception {
        when(cookingService.cook(3L))
                .thenThrow(new InsufficientIngredientsException("Tomato pasta", List.of("Pasta (200 g)")));

        mockMvc.perform(post("/recipes/3/cook"))
                .andExpect(redirectedUrl("/recipes/3"))
                .andExpect(flash().attribute(FlashMessages.ERROR_ATTRIBUTE, "Not enough ingredients: Pasta (200 g)"));
    }

    @Test
    void missingIngredientsAreAddedToShoppingList() throws Exception {
        when(shoppingListService.addMissingIngredients(3L)).thenReturn(1, 0);

        mockMvc.perform(post("/recipes/3/shopping-list"))
                .andExpect(redirectedUrl("/recipes/3"))
                .andExpect(flash().attribute(FlashMessages.SUCCESS_ATTRIBUTE, "1 missing ingredient(s) added to the shopping list"));
        mockMvc.perform(post("/recipes/3/shopping-list"))
                .andExpect(flash().attribute(FlashMessages.SUCCESS_ATTRIBUTE, "You already have everything for this recipe"));
    }
}
