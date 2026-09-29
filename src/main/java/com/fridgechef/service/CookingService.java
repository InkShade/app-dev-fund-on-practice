package com.fridgechef.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fridgechef.domain.FridgeItem;
import com.fridgechef.dto.IngredientRequirement;
import com.fridgechef.dto.RecipeMatch;
import com.fridgechef.exception.InsufficientIngredientsException;
import com.fridgechef.repository.FridgeItemRepository;

/** Cooks a recipe by taking its ingredients out of the fridge. */
@Service
public class CookingService {

    /** Leftovers smaller than this are treated as nothing (protects against floating point noise). */
    private static final double EPSILON = 1e-9;

    private final RecipeMatchingService recipeMatchingService;
    private final FridgeItemRepository fridgeItemRepository;
    private final FreshnessPolicy freshnessPolicy;

    public CookingService(RecipeMatchingService recipeMatchingService,
                          FridgeItemRepository fridgeItemRepository,
                          FreshnessPolicy freshnessPolicy) {
        this.recipeMatchingService = recipeMatchingService;
        this.fridgeItemRepository = fridgeItemRepository;
        this.freshnessPolicy = freshnessPolicy;
    }

    /**
     * Takes every ingredient of the recipe out of the fridge. Products that expire first are used first
     * (FEFO), and products that are used up are removed.
     *
     * @throws InsufficientIngredientsException when the fridge does not have enough of some ingredient
     */
    @Transactional
    public RecipeMatch cook(Long recipeId) {
        RecipeMatch match = recipeMatchingService.getMatch(recipeId);
        if (!match.canCook()) {
            List<String> missing = match.missingIngredients().stream()
                    .map(requirement -> requirement.ingredientName() + " (" + requirement.missingLabel() + ")")
                    .toList();
            throw new InsufficientIngredientsException(match.name(), missing);
        }
        LocalDate today = freshnessPolicy.today();
        for (IngredientRequirement requirement : match.requirements()) {
            takeFromFridge(requirement.ingredientId(), requirement.required(), today);
        }
        return match;
    }

    private void takeFromFridge(Long ingredientId, double amount, LocalDate today) {
        double remaining = amount;
        List<FridgeItem> usableItems = fridgeItemRepository
                .findByIngredientIdAndExpiryDateGreaterThanEqualOrderByExpiryDateAscIdAsc(ingredientId, today);
        for (FridgeItem item : usableItems) {
            if (remaining < EPSILON) {
                return;
            }
            remaining -= item.consume(remaining);
            if (item.getQuantity() < EPSILON) {
                fridgeItemRepository.delete(item);
            }
        }
    }
}
