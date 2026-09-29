package com.fridgechef.domain;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "fridge_item")
public class FridgeItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ingredient_id", nullable = false)
    private Ingredient ingredient;

    @Column(nullable = false)
    private double quantity;

    @Column(name = "expiry_date", nullable = false)
    private LocalDate expiryDate;

    protected FridgeItem() {
        // required by JPA
    }

    public FridgeItem(Ingredient ingredient, double quantity, LocalDate expiryDate) {
        this.ingredient = ingredient;
        this.quantity = quantity;
        this.expiryDate = expiryDate;
    }

    /**
     * Takes up to {@code requested} from this item.
     *
     * @return the amount actually taken
     */
    public double consume(double requested) {
        double taken = Math.min(quantity, requested);
        quantity -= taken;
        return taken;
    }

    public Long getId() {
        return id;
    }

    public Ingredient getIngredient() {
        return ingredient;
    }

    public double getQuantity() {
        return quantity;
    }

    public LocalDate getExpiryDate() {
        return expiryDate;
    }
}
