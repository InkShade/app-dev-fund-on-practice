package com.fridgechef.web;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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
import com.fridgechef.dto.ShoppingListItemView;
import com.fridgechef.service.IngredientService;
import com.fridgechef.service.ShoppingListService;

@WebMvcTest(ShoppingListController.class)
class ShoppingListControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ShoppingListService shoppingListService;

    @MockitoBean
    private IngredientService ingredientService;

    @Test
    void listShowsItems() throws Exception {
        when(shoppingListService.findAll()).thenReturn(List.of(
                new ShoppingListItemView(1L, "Honey", 250, MeasureUnit.GRAM, false)));

        mockMvc.perform(get("/shopping-list"))
                .andExpect(status().isOk())
                .andExpect(view().name("shopping-list"))
                .andExpect(content().string(containsString("Honey")))
                .andExpect(content().string(containsString("250 g")));
    }

    @Test
    void validItemIsAdded() throws Exception {
        mockMvc.perform(post("/shopping-list").param("ingredientId", "1").param("quantity", "2"))
                .andExpect(redirectedUrl("/shopping-list"))
                .andExpect(flash().attribute(FlashMessages.SUCCESS, "Added to the shopping list"));
        verify(shoppingListService).add(any());
    }

    @Test
    void invalidItemRedisplaysForm() throws Exception {
        mockMvc.perform(post("/shopping-list").param("ingredientId", "1"))
                .andExpect(status().isOk())
                .andExpect(view().name("shopping-list"))
                .andExpect(model().attributeHasFieldErrors("shoppingItemForm", "quantity"));
        verify(shoppingListService, never()).add(any());
    }

    @Test
    void itemCanBeToggledAndRemoved() throws Exception {
        mockMvc.perform(post("/shopping-list/4/toggle")).andExpect(redirectedUrl("/shopping-list"));
        mockMvc.perform(post("/shopping-list/4/delete")).andExpect(redirectedUrl("/shopping-list"));

        verify(shoppingListService).togglePurchased(4L);
        verify(shoppingListService).remove(4L);
    }

    @Test
    void movingToFridgeRequiresPurchasedItems() throws Exception {
        when(shoppingListService.movePurchasedToFridge()).thenReturn(0, 3);

        mockMvc.perform(post("/shopping-list/move-to-fridge"))
                .andExpect(flash().attribute(FlashMessages.ERROR, "Tick the products you have bought first"));
        mockMvc.perform(post("/shopping-list/move-to-fridge"))
                .andExpect(flash().attribute(FlashMessages.SUCCESS, "3 product(s) moved into the fridge"));
    }
}
