package com.fridgechef.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.fridgechef.domain.Recipe;

public interface RecipeRepository extends JpaRepository<Recipe, Long> {

    @EntityGraph(attributePaths = {"ingredients", "ingredients.ingredient"})
    List<Recipe> findAllByOrderByNameAsc();

    @EntityGraph(attributePaths = {"ingredients", "ingredients.ingredient"})
    Optional<Recipe> findWithIngredientsById(Long id);

    @Query("select count(distinct r) from Recipe r join r.ingredients ri where ri.ingredient.id = :ingredientId")
    long countUsingIngredient(@Param("ingredientId") Long ingredientId);

    @Query("select ri.ingredient.id as ingredientId, count(distinct r) as total "
            + "from Recipe r join r.ingredients ri group by ri.ingredient.id")
    List<IngredientCount> countPerIngredient();
}
