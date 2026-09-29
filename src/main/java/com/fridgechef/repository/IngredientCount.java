package com.fridgechef.repository;

/** Projection for "how many rows reference each ingredient" queries. */
public interface IngredientCount {

    Long getIngredientId();

    long getTotal();
}
