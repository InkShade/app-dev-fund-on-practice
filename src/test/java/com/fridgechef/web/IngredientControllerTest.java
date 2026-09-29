package com.fridgechef.web;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
import com.fridgechef.dto.IngredientOverview;
import com.fridgechef.dto.IngredientUsage;
import com.fridgechef.dto.IngredientView;
import com.fridgechef.exception.DuplicateIngredientException;
import com.fridgechef.exception.IngredientInUseException;
import com.fridgechef.exception.ResourceNotFoundException;
import com.fridgechef.service.IngredientService;

@WebMvcTest(IngredientController.class)
class IngredientControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private IngredientService ingredientService;

    private static final IngredientView MILK = new IngredientView(1L, "Milk", MeasureUnit.MILLILITER, 7);
    private static final IngredientUsage USED = new IngredientUsage(2, 1, 0);
    private static final IngredientUsage UNUSED = new IngredientUsage(0, 0, 0);

    @Test
    void listShowsCatalogueWithUsage() throws Exception {
        when(ingredientService.findAllWithUsage()).thenReturn(List.of(
                new IngredientOverview(MILK, USED),
                new IngredientOverview(new IngredientView(2L, "Honey", MeasureUnit.GRAM, 365), UNUSED)));

        mockMvc.perform(get("/ingredients"))
                .andExpect(status().isOk())
                .andExpect(view().name("ingredients"))
                .andExpect(content().string(containsString("Milk")))
                .andExpect(content().string(containsString("7 days")))
                .andExpect(content().string(containsString("2 recipes, 1 fridge item")))
                .andExpect(content().string(containsString("not used")))
                .andExpect(content().string(containsString("/ingredients/2/edit")));
    }

    @Test
    void editPageIsPrefilled() throws Exception {
        when(ingredientService.getOverview(1L)).thenReturn(new IngredientOverview(MILK, USED));

        mockMvc.perform(get("/ingredients/1/edit"))
                .andExpect(status().isOk())
                .andExpect(view().name("ingredient-edit"))
                .andExpect(model().attribute("ingredientForm", hasProperty("name", is("Milk"))))
                .andExpect(content().string(containsString("Can only be changed while the ingredient is not used")));
    }

    @Test
    void editOfUnknownIngredientShowsNotFound() throws Exception {
        when(ingredientService.getOverview(99L)).thenThrow(new ResourceNotFoundException("Ingredient", 99L));

        mockMvc.perform(get("/ingredients/99/edit"))
                .andExpect(status().isNotFound())
                .andExpect(view().name("error/not-found"));
    }

    @Test
    void validUpdateRedirectsToCatalogue() throws Exception {
        when(ingredientService.update(eq(1L), any())).thenReturn(new IngredientView(1L, "Oat milk", MeasureUnit.MILLILITER, 10));

        mockMvc.perform(post("/ingredients/1")
                        .param("name", "Oat milk")
                        .param("unit", "MILLILITER")
                        .param("shelfLifeDays", "10"))
                .andExpect(redirectedUrl("/ingredients"))
                .andExpect(flash().attribute(FlashMessages.SUCCESS_ATTRIBUTE, "Oat milk was updated"));
    }

    @Test
    void invalidUpdateShowsFormAgain() throws Exception {
        when(ingredientService.getOverview(1L)).thenReturn(new IngredientOverview(MILK, USED));

        mockMvc.perform(post("/ingredients/1").param("name", "").param("unit", "MILLILITER").param("shelfLifeDays", "7"))
                .andExpect(status().isOk())
                .andExpect(view().name("ingredient-edit"))
                .andExpect(model().attributeHasFieldErrors("ingredientForm", "name"));
        verify(ingredientService, never()).update(any(), any());
    }

    @Test
    void updateToExistingNameIsReportedOnTheField() throws Exception {
        when(ingredientService.update(eq(1L), any())).thenThrow(new DuplicateIngredientException("Eggs"));
        when(ingredientService.getOverview(1L)).thenReturn(new IngredientOverview(MILK, USED));

        mockMvc.perform(post("/ingredients/1").param("name", "Eggs").param("unit", "MILLILITER").param("shelfLifeDays", "7"))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrorCode("ingredientForm", "name", "duplicate"));
    }

    @Test
    void unitChangeOfUsedIngredientIsReportedOnTheField() throws Exception {
        when(ingredientService.update(eq(1L), any()))
                .thenThrow(new IngredientInUseException("change the unit of", "Milk", "2 recipes"));
        when(ingredientService.getOverview(1L)).thenReturn(new IngredientOverview(MILK, USED));

        mockMvc.perform(post("/ingredients/1").param("name", "Milk").param("unit", "GRAM").param("shelfLifeDays", "7"))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrorCode("ingredientForm", "unit", "inUse"))
                .andExpect(content().string(containsString("Cannot change the unit of Milk: it is used in 2 recipes")));
    }

    @Test
    void unusedIngredientIsDeleted() throws Exception {
        when(ingredientService.delete(2L)).thenReturn("Honey");

        mockMvc.perform(post("/ingredients/2/delete"))
                .andExpect(redirectedUrl("/ingredients"))
                .andExpect(flash().attribute(FlashMessages.SUCCESS_ATTRIBUTE, "Honey was removed from the catalogue"));
    }

    @Test
    void deletingUsedIngredientShowsError() throws Exception {
        when(ingredientService.delete(1L)).thenThrow(new IngredientInUseException("delete", "Milk", "2 recipes"));

        mockMvc.perform(post("/ingredients/1/delete"))
                .andExpect(redirectedUrl("/ingredients"))
                .andExpect(flash().attribute(FlashMessages.ERROR_ATTRIBUTE, "Cannot delete Milk: it is used in 2 recipes"));
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
