package com.fridgechef.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "recipe")
public class Recipe {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 150)
    private String name;

    @Column(length = 500)
    private String description;

    @Column(length = 4000)
    private String instructions;

    @Column(name = "cooking_time_minutes", nullable = false)
    private int cookingTimeMinutes;

    @OneToMany(mappedBy = "recipe", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RecipeIngredient> ingredients = new ArrayList<>();

    protected Recipe() {
        // required by JPA
    }

    public Recipe(String name, String description, String instructions, int cookingTimeMinutes) {
        this.name = name;
        this.description = description;
        this.instructions = instructions;
        this.cookingTimeMinutes = cookingTimeMinutes;
    }

    public Recipe addIngredient(Ingredient ingredient, double quantity) {
        ingredients.add(new RecipeIngredient(this, ingredient, quantity));
        return this;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getInstructions() {
        return instructions;
    }

    public int getCookingTimeMinutes() {
        return cookingTimeMinutes;
    }

    public List<RecipeIngredient> getIngredients() {
        return Collections.unmodifiableList(ingredients);
    }
}
