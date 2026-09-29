package com.fridgechef.web;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.util.List;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fridgechef.TestData;
import com.fridgechef.domain.FreshnessStatus;
import com.fridgechef.domain.MeasureUnit;
import com.fridgechef.dto.FridgeItemView;
import com.fridgechef.dto.FridgeSummary;
import com.fridgechef.dto.RecipeMatch;
import com.fridgechef.service.FridgeService;
import com.fridgechef.service.RecipeMatchingService;

@WebMvcTest(HomeController.class)
class HomeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FridgeService fridgeService;

    @MockitoBean
    private RecipeMatchingService recipeMatchingService;

    @Test
    void dashboardShowsSummaryExpiringProductsAndTopThreeMatches() throws Exception {
        when(fridgeService.summary()).thenReturn(new FridgeSummary(10, 2, 1));
        when(fridgeService.findExpiringSoon()).thenReturn(List.of(new FridgeItemView(
                1L, "Milk", 500, MeasureUnit.MILLILITER, TestData.TODAY, 0, FreshnessStatus.EXPIRING_SOON)));
        List<RecipeMatch> matches = IntStream.rangeClosed(1, 5)
                .mapToObj(i -> new RecipeMatch((long) i, "Recipe " + i, "d", "s", 10, 100, 0, List.of()))
                .toList();
        when(recipeMatchingService.findMatches(1)).thenReturn(matches);

        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"))
                .andExpect(model().attribute("topMatches", hasSize(HomeController.TOP_MATCHES)))
                .andExpect(content().string(containsString("expires today")))
                .andExpect(content().string(containsString("Recipe 3")));
    }
}
