package com.fridgechef.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.fridgechef.domain.Ingredient;

public interface IngredientRepository extends JpaRepository<Ingredient, Long> {

    List<Ingredient> findAllByOrderByNameAsc();

    boolean existsByNameIgnoreCase(String name);
}
