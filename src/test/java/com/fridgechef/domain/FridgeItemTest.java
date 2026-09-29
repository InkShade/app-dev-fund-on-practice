package com.fridgechef.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

class FridgeItemTest {

    private final Ingredient milk = new Ingredient("Milk", MeasureUnit.MILLILITER, 7);

    @Test
    void consumeTakesRequestedAmountWhenEnoughIsLeft() {
        FridgeItem item = new FridgeItem(milk, 500, LocalDate.now());

        assertThat(item.consume(200)).isEqualTo(200);
        assertThat(item.getQuantity()).isEqualTo(300);
    }

    @Test
    void consumeNeverTakesMoreThanIsLeft() {
        FridgeItem item = new FridgeItem(milk, 150, LocalDate.now());

        assertThat(item.consume(200)).isEqualTo(150);
        assertThat(item.getQuantity()).isZero();
    }

    @Test
    void shoppingListItemAccumulatesQuantityAndTogglesPurchased() {
        ShoppingListItem item = new ShoppingListItem(milk, 500);

        item.increaseQuantity(250);
        item.togglePurchased();

        assertThat(item.getQuantity()).isEqualTo(750);
        assertThat(item.isPurchased()).isTrue();
        item.togglePurchased();
        assertThat(item.isPurchased()).isFalse();
    }
}
