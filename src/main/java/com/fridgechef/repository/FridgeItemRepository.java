package com.fridgechef.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.fridgechef.domain.FridgeItem;

public interface FridgeItemRepository extends JpaRepository<FridgeItem, Long> {

    @EntityGraph(attributePaths = "ingredient")
    List<FridgeItem> findAllByOrderByExpiryDateAscIdAsc();

    /** Usable (not expired) portions of an ingredient, the ones expiring first come first. */
    List<FridgeItem> findByIngredientIdAndExpiryDateGreaterThanEqualOrderByExpiryDateAscIdAsc(
            Long ingredientId, LocalDate date);

    long deleteByExpiryDateBefore(LocalDate date);

    long countByIngredientId(Long ingredientId);

    @Query("select f.ingredient.id as ingredientId, count(f) as total from FridgeItem f group by f.ingredient.id")
    List<IngredientCount> countPerIngredient();
}
