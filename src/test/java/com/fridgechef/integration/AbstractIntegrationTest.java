package com.fridgechef.integration;

import java.time.Clock;

import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.fridgechef.TestData;
import com.fridgechef.domain.Ingredient;
import com.fridgechef.domain.MeasureUnit;
import com.fridgechef.repository.FridgeItemRepository;
import com.fridgechef.repository.IngredientRepository;
import com.fridgechef.repository.RecipeRepository;
import com.fridgechef.repository.ShoppingListItemRepository;

/** Boots the whole application against an empty in-memory database with a fixed clock. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(AbstractIntegrationTest.FixedClockConfig.class)
abstract class AbstractIntegrationTest {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected IngredientRepository ingredientRepository;

    @Autowired
    protected FridgeItemRepository fridgeItemRepository;

    @Autowired
    protected RecipeRepository recipeRepository;

    @Autowired
    protected ShoppingListItemRepository shoppingListItemRepository;

    @AfterEach
    void cleanDatabase() {
        shoppingListItemRepository.deleteAll();
        fridgeItemRepository.deleteAll();
        recipeRepository.deleteAll();
        ingredientRepository.deleteAll();
    }

    protected Ingredient saveIngredient(String name, MeasureUnit unit, int shelfLifeDays) {
        return ingredientRepository.save(new Ingredient(name, unit, shelfLifeDays));
    }

    @TestConfiguration
    static class FixedClockConfig {

        @Bean
        @Primary
        Clock fixedClock() {
            return TestData.fixedClock();
        }
    }
}
