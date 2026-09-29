package com.fridgechef.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.fridgechef.repository.FridgeItemRepository;
import com.fridgechef.repository.IngredientRepository;
import com.fridgechef.repository.RecipeRepository;

/** Makes sure data.sql stays valid, so the demo always starts. */
@SpringBootTest(properties = {
        "spring.sql.init.mode=embedded",
        "spring.datasource.url=jdbc:h2:mem:fridgechef-demo;DB_CLOSE_DELAY=-1"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DemoDataIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private IngredientRepository ingredientRepository;

    @Autowired
    private RecipeRepository recipeRepository;

    @Autowired
    private FridgeItemRepository fridgeItemRepository;

    @Test
    void demoDataIsLoaded() {
        assertThat(ingredientRepository.count()).isEqualTo(21);
        assertThat(recipeRepository.findAllByOrderByNameAsc())
                .hasSize(12)
                .allSatisfy(recipe -> assertThat(recipe.getIngredients()).isNotEmpty());
        assertThat(fridgeItemRepository.count()).isEqualTo(17);
    }

    @Test
    void everyPageRendersWithDemoData() throws Exception {
        mockMvc.perform(get("/")).andExpect(status().isOk())
                .andExpect(content().string(containsString("Use these first")));
        mockMvc.perform(get("/fridge")).andExpect(status().isOk());
        mockMvc.perform(get("/recipes")).andExpect(status().isOk())
                .andExpect(content().string(containsString("Classic omelette")));
        mockMvc.perform(get("/shopping-list")).andExpect(status().isOk())
                .andExpect(content().string(containsString("Honey")));
        mockMvc.perform(get("/ingredients")).andExpect(status().isOk());
    }
}
