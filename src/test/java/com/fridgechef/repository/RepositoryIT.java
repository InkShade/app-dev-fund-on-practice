package com.fridgechef.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import com.fridgechef.domain.FridgeItem;
import com.fridgechef.domain.Ingredient;
import com.fridgechef.domain.MeasureUnit;
import com.fridgechef.domain.Recipe;
import com.fridgechef.domain.ShoppingListItem;

@DataJpaTest
@ActiveProfiles("test")
class RepositoryIT {

    private static final LocalDate TODAY = LocalDate.of(2026, 9, 29);

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private IngredientRepository ingredientRepository;

    @Autowired
    private FridgeItemRepository fridgeItemRepository;

    @Autowired
    private RecipeRepository recipeRepository;

    @Autowired
    private ShoppingListItemRepository shoppingListItemRepository;

    private Ingredient milk;
    private Ingredient eggs;

    @BeforeEach
    void setUp() {
        milk = entityManager.persist(new Ingredient("Milk", MeasureUnit.MILLILITER, 7));
        eggs = entityManager.persist(new Ingredient("Eggs", MeasureUnit.PIECE, 21));
    }

    @Test
    void ingredientsAreSortedByNameAndLookedUpIgnoringCase() {
        assertThat(ingredientRepository.findAllByOrderByNameAsc())
                .extracting(Ingredient::getName)
                .containsExactly("Eggs", "Milk");
        assertThat(ingredientRepository.existsByNameIgnoreCase("mILK")).isTrue();
        assertThat(ingredientRepository.existsByNameIgnoreCase("Butter")).isFalse();
        assertThat(ingredientRepository.existsByNameIgnoreCaseAndIdNot("milk", milk.getId())).isFalse();
        assertThat(ingredientRepository.existsByNameIgnoreCaseAndIdNot("milk", eggs.getId())).isTrue();
    }

    @Test
    void ingredientUsageIsCountedPerTable() {
        entityManager.persist(new Recipe("Omelette", "Quick breakfast", "Whisk and fry", 10)
                .addIngredient(eggs, 3)
                .addIngredient(milk, 50));
        entityManager.persist(new Recipe("Boiled eggs", "Simple", "Boil", 10).addIngredient(eggs, 2));
        entityManager.persist(new FridgeItem(eggs, 6, TODAY));
        entityManager.persist(new FridgeItem(eggs, 4, TODAY.plusDays(3)));
        entityManager.persist(new ShoppingListItem(milk, 1000));

        assertThat(recipeRepository.countUsingIngredient(eggs.getId())).isEqualTo(2);
        assertThat(fridgeItemRepository.countByIngredientId(eggs.getId())).isEqualTo(2);
        assertThat(shoppingListItemRepository.countByIngredientId(eggs.getId())).isZero();
        assertThat(recipeRepository.countPerIngredient())
                .extracting(IngredientCount::getIngredientId, IngredientCount::getTotal)
                .containsExactlyInAnyOrder(tuple(eggs.getId(), 2L), tuple(milk.getId(), 1L));
        assertThat(fridgeItemRepository.countPerIngredient())
                .extracting(IngredientCount::getIngredientId, IngredientCount::getTotal)
                .containsExactly(tuple(eggs.getId(), 2L));
        assertThat(shoppingListItemRepository.countPerIngredient())
                .extracting(IngredientCount::getIngredientId, IngredientCount::getTotal)
                .containsExactly(tuple(milk.getId(), 1L));
    }

    @Test
    void usableFridgeItemsAreReturnedFirstExpiringFirst() {
        entityManager.persist(new FridgeItem(milk, 500, TODAY.plusDays(5)));
        entityManager.persist(new FridgeItem(milk, 200, TODAY.plusDays(1)));
        entityManager.persist(new FridgeItem(milk, 300, TODAY.minusDays(1)));
        entityManager.persist(new FridgeItem(eggs, 6, TODAY));

        assertThat(fridgeItemRepository
                .findByIngredientIdAndExpiryDateGreaterThanEqualOrderByExpiryDateAscIdAsc(milk.getId(), TODAY))
                .extracting(FridgeItem::getQuantity)
                .containsExactly(200.0, 500.0);
    }

    @Test
    void expiredItemsCanBeDeletedInBulk() {
        entityManager.persist(new FridgeItem(milk, 300, TODAY.minusDays(2)));
        entityManager.persist(new FridgeItem(eggs, 6, TODAY));

        long deleted = fridgeItemRepository.deleteByExpiryDateBefore(TODAY);

        assertThat(deleted).isEqualTo(1);
        assertThat(fridgeItemRepository.findAllByOrderByExpiryDateAscIdAsc())
                .extracting(item -> item.getIngredient().getName())
                .containsExactly("Eggs");
    }

    @Test
    void recipeIsLoadedTogetherWithIngredients() {
        Recipe omelette = new Recipe("Omelette", "Quick breakfast", "Whisk and fry", 10)
                .addIngredient(eggs, 3)
                .addIngredient(milk, 50);
        Long id = entityManager.persistAndFlush(omelette).getId();
        entityManager.clear();

        Recipe loaded = recipeRepository.findWithIngredientsById(id).orElseThrow();

        assertThat(loaded.getIngredients())
                .extracting(ri -> ri.getIngredient().getName(), ri -> ri.getQuantity())
                .containsExactlyInAnyOrder(
                        tuple("Eggs", 3.0),
                        tuple("Milk", 50.0));
        assertThat(recipeRepository.findAllByOrderByNameAsc()).hasSize(1);
    }

    @Test
    void openShoppingListItemIsFoundByIngredient() {
        ShoppingListItem bought = new ShoppingListItem(milk, 1000);
        bought.togglePurchased();
        entityManager.persist(bought);
        entityManager.persist(new ShoppingListItem(eggs, 10));

        assertThat(shoppingListItemRepository.findByIngredientIdAndPurchasedFalse(eggs.getId())).isPresent();
        assertThat(shoppingListItemRepository.findByIngredientIdAndPurchasedFalse(milk.getId())).isEmpty();
        assertThat(shoppingListItemRepository.findByPurchasedTrue())
                .extracting(item -> item.getIngredient().getName())
                .containsExactly("Milk");
        assertThat(shoppingListItemRepository.findAllByOrderByPurchasedAscIdAsc())
                .extracting(item -> item.getIngredient().getName())
                .containsExactly("Eggs", "Milk");
    }
}
