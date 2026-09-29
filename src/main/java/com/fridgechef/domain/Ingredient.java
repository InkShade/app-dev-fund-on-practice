package com.fridgechef.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "ingredient")
public class Ingredient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MeasureUnit unit;

    /** How many days a freshly bought product usually stays good. */
    @Column(name = "shelf_life_days", nullable = false)
    private int shelfLifeDays;

    protected Ingredient() {
        // required by JPA
    }

    public Ingredient(String name, MeasureUnit unit, int shelfLifeDays) {
        this.name = name;
        this.unit = unit;
        this.shelfLifeDays = shelfLifeDays;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public MeasureUnit getUnit() {
        return unit;
    }

    public int getShelfLifeDays() {
        return shelfLifeDays;
    }

    public void update(String name, MeasureUnit unit, int shelfLifeDays) {
        this.name = name;
        this.unit = unit;
        this.shelfLifeDays = shelfLifeDays;
    }
}
