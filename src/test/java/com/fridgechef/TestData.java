package com.fridgechef;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;

import org.springframework.test.util.ReflectionTestUtils;

import com.fridgechef.config.FridgeChefProperties;
import com.fridgechef.domain.FridgeItem;
import com.fridgechef.domain.Ingredient;
import com.fridgechef.domain.MeasureUnit;
import com.fridgechef.domain.Recipe;
import com.fridgechef.domain.ShoppingListItem;
import com.fridgechef.service.FreshnessPolicy;

/** Factory methods for entities with ids, so unit tests do not need a database. */
public final class TestData {

    public static final LocalDate TODAY = LocalDate.of(2026, 9, 29);
    public static final int EXPIRING_SOON_DAYS = 3;

    private TestData() {
    }

    public static Clock fixedClock() {
        return Clock.fixed(TODAY.atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
    }

    public static FreshnessPolicy freshnessPolicy() {
        return new FreshnessPolicy(fixedClock(), new FridgeChefProperties(EXPIRING_SOON_DAYS));
    }

    public static Ingredient ingredient(long id, String name, MeasureUnit unit) {
        return withId(new Ingredient(name, unit, 7), id);
    }

    public static Ingredient ingredient(long id, String name, MeasureUnit unit, int shelfLifeDays) {
        return withId(new Ingredient(name, unit, shelfLifeDays), id);
    }

    public static FridgeItem fridgeItem(long id, Ingredient ingredient, double quantity, LocalDate expiryDate) {
        return withId(new FridgeItem(ingredient, quantity, expiryDate), id);
    }

    public static Recipe recipe(long id, String name) {
        return withId(new Recipe(name, name + " description", "Cook " + name, 20), id);
    }

    public static ShoppingListItem shoppingItem(long id, Ingredient ingredient, double quantity) {
        return withId(new ShoppingListItem(ingredient, quantity), id);
    }

    private static <T> T withId(T entity, long id) {
        ReflectionTestUtils.setField(entity, "id", id);
        return entity;
    }
}
