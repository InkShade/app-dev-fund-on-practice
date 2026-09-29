package com.fridgechef.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.fridgechef.domain.ShoppingListItem;

public interface ShoppingListItemRepository extends JpaRepository<ShoppingListItem, Long> {

    @EntityGraph(attributePaths = "ingredient")
    List<ShoppingListItem> findAllByOrderByPurchasedAscIdAsc();

    Optional<ShoppingListItem> findByIngredientIdAndPurchasedFalse(Long ingredientId);

    @EntityGraph(attributePaths = "ingredient")
    List<ShoppingListItem> findByPurchasedTrue();

    long countByIngredientId(Long ingredientId);

    @Query("select s.ingredient.id as ingredientId, count(s) as total from ShoppingListItem s group by s.ingredient.id")
    List<IngredientCount> countPerIngredient();
}
