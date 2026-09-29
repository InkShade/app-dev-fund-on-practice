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
import com.fridgechef.dto.IngredientView;
import com.fridgechef.exception.DuplicateIngredientException;
import com.fridgechef.service.IngredientService;

@WebMvcTest(IngredientController.class)
class IngredientControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private IngredientService ingredientService;

    @Test
    void listShowsCatalogue() throws Exception {
        when(ingredientService.findAll()).thenReturn(List.of(new IngredientView(1L, "Milk", MeasureUnit.MILLILITER, 7)));

        mockMvc.perform(get("/ingredients"))
                .andExpect(status().isOk())
                .andExpect(view().name("ingredients"))
                .andExpect(content().string(containsString("Milk")))
                .andExpect(content().string(containsString("7 days")));
    }

    @Test
    void validIngredientIsCreated() throws Exception {
        when(ingredientService.create(any())).thenReturn(new IngredientView(2L, "Butter", MeasureUnit.GRAM, 30));

        mockMvc.perform(post("/ingredients")
                        .param("name", "Butter")
                        .param("unit", "GRAM")
                        .param("shelfLifeDays", "30"))
                .andExpect(redirectedUrl("/ingredients"))
                .andExpect(flash().attribute(FlashMessages.SUCCESS_ATTRIBUTE, "Butter was added to the catalogue"));
    }

    @Test
    void blankNameIsRejected() throws Exception {
        mockMvc.perform(post("/ingredients").param("name", " ").param("unit", "GRAM").param("shelfLifeDays", "0"))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("ingredientForm", "name", "shelfLifeDays"));
        verify(ingredientService, never()).create(any());
    }

    @Test
    void duplicateNameIsReportedOnTheField() throws Exception {
        when(ingredientService.create(any())).thenThrow(new DuplicateIngredientException("Milk"));

        mockMvc.perform(post("/ingredients")
                        .param("name", "Milk")
                        .param("unit", "MILLILITER")
                        .param("shelfLifeDays", "7"))
                .andExpect(status().isOk())
                .andExpect(view().name("ingredients"))
                .andExpect(model().attributeHasFieldErrorCode("ingredientForm", "name", "duplicate"))
                .andExpect(content().string(containsString("Ingredient &#39;Milk&#39; already exists")));
    }
}
