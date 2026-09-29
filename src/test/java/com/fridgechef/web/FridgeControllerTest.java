package com.fridgechef.web;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
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

import com.fridgechef.TestData;
import com.fridgechef.domain.FreshnessStatus;
import com.fridgechef.domain.MeasureUnit;
import com.fridgechef.dto.FridgeItemView;
import com.fridgechef.dto.IngredientView;
import com.fridgechef.exception.ResourceNotFoundException;
import com.fridgechef.service.FridgeService;
import com.fridgechef.service.IngredientService;

@WebMvcTest(FridgeController.class)
class FridgeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FridgeService fridgeService;

    @MockitoBean
    private IngredientService ingredientService;

    private final FridgeItemView milk = new FridgeItemView(
            1L, "Milk", 500, MeasureUnit.MILLILITER, TestData.TODAY.plusDays(5), 5, FreshnessStatus.FRESH);

    @Test
    void listShowsFridgeContents() throws Exception {
        when(fridgeService.findAll()).thenReturn(List.of(milk));
        when(ingredientService.findAll()).thenReturn(List.of(new IngredientView(1L, "Milk", MeasureUnit.MILLILITER, 7)));

        mockMvc.perform(get("/fridge"))
                .andExpect(status().isOk())
                .andExpect(view().name("fridge"))
                .andExpect(content().string(containsString("500 ml")))
                .andExpect(content().string(containsString("5 days left")));
    }

    @Test
    void validProductIsAddedAndUserIsRedirected() throws Exception {
        when(fridgeService.add(any())).thenReturn(milk);

        mockMvc.perform(post("/fridge")
                        .param("ingredientId", "1")
                        .param("quantity", "500")
                        .param("expiryDate", "2026-10-04"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/fridge"))
                .andExpect(flash().attribute(FlashMessages.SUCCESS_ATTRIBUTE, "500 ml of Milk put into the fridge"));
    }

    @Test
    void invalidProductRedisplaysFormWithErrors() throws Exception {
        mockMvc.perform(post("/fridge").param("quantity", "-3"))
                .andExpect(status().isOk())
                .andExpect(view().name("fridge"))
                .andExpect(model().attributeHasFieldErrors("fridgeItemForm", "ingredientId", "quantity"))
                .andExpect(content().string(containsString("Quantity must be greater than zero")));
        verify(fridgeService, never()).add(any());
    }

    @Test
    void productCanBeRemoved() throws Exception {
        mockMvc.perform(post("/fridge/1/delete"))
                .andExpect(redirectedUrl("/fridge"))
                .andExpect(flash().attributeExists(FlashMessages.SUCCESS_ATTRIBUTE));
        verify(fridgeService).remove(1L);
    }

    @Test
    void removingUnknownProductShowsNotFoundPage() throws Exception {
        doThrow(new ResourceNotFoundException("Fridge item", 9L)).when(fridgeService).remove(9L);

        mockMvc.perform(post("/fridge/9/delete"))
                .andExpect(status().isNotFound())
                .andExpect(view().name("error/not-found"))
                .andExpect(content().string(containsString("Fridge item with id 9 was not found")));
    }

    @Test
    void discardExpiredReportsHowManyWereThrownAway() throws Exception {
        when(fridgeService.discardExpired()).thenReturn(2L, 0L);

        mockMvc.perform(post("/fridge/discard-expired"))
                .andExpect(flash().attribute(FlashMessages.SUCCESS_ATTRIBUTE, "Threw away 2 expired product(s)"));
        mockMvc.perform(post("/fridge/discard-expired"))
                .andExpect(flash().attribute(FlashMessages.SUCCESS_ATTRIBUTE, "Nothing to throw away, everything is still good"));
    }
}
