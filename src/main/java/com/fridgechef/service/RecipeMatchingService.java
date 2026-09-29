package com.fridgechef.service;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fridgechef.domain.FreshnessStatus;
import com.fridgechef.domain.FridgeItem;
import com.fridgechef.domain.Ingredient;
import com.fridgechef.domain.Recipe;
import com.fridgechef.domain.RecipeIngredient;
import com.fridgechef.dto.IngredientRequirement;
import com.fridgechef.dto.RecipeMatch;
import com.fridgechef.exception.ResourceNotFoundException;
import com.fridgechef.repository.FridgeItemRepository;
import com.fridgechef.repository.RecipeRepository;

/**
 * Scores recipes against the fridge contents.
 *
 * <p>Recipes are ranked by the share of ingredients available in the needed quantity. Ties are broken
 * in favour of recipes that use up products which expire soon, so less food goes to waste.
 * Expired products are never counted as available.</p>
 */
@Service
@Transactional(readOnly = true)
public class RecipeMatchingService {

    private static final Comparator<RecipeMatch> BEST_FIRST =
            Comparator.comparingInt(RecipeMatch::matchPercent).reversed()
                    .thenComparing(Comparator.comparingInt(RecipeMatch::expiringIngredientsUsed).reversed())
                    .thenComparing(RecipeMatch::name);

    private final RecipeRepository recipeRepository;
    private final FridgeItemRepository fridgeItemRepository;
    private final FreshnessPolicy freshnessPolicy;

    public RecipeMatchingService(RecipeRepository recipeRepository,
                                 FridgeItemRepository fridgeItemRepository,
                                 FreshnessPolicy freshnessPolicy) {
        this.recipeRepository = recipeRepository;
        this.fridgeItemRepository = fridgeItemRepository;
        this.freshnessPolicy = freshnessPolicy;
    }

    /** Recipes matching at least {@code minMatchPercent} percent, best matches first. */
    public List<RecipeMatch> findMatches(int minMatchPercent) {
        FridgeStock stock = currentStock();
        return recipeRepository.findAllByOrderByNameAsc().stream()
                .map(recipe -> match(recipe, stock))
                .filter(match -> match.matchPercent() >= minMatchPercent)
                .sorted(BEST_FIRST)
                .toList();
    }

    public RecipeMatch getMatch(Long recipeId) {
        Recipe recipe = recipeRepository.findWithIngredientsById(recipeId)
                .orElseThrow(() -> new ResourceNotFoundException("Recipe", recipeId));
        return match(recipe, currentStock());
    }

    static int matchPercent(List<IngredientRequirement> requirements) {
        if (requirements.isEmpty()) {
            return 100;
        }
        long satisfied = requirements.stream().filter(IngredientRequirement::isSatisfied).count();
        return (int) (satisfied * 100 / requirements.size());
    }

    private FridgeStock currentStock() {
        Map<Long, Double> available = new HashMap<>();
        Set<Long> expiringSoon = new HashSet<>();
        for (FridgeItem item : fridgeItemRepository.findAllByOrderByExpiryDateAscIdAsc()) {
            FreshnessStatus status = freshnessPolicy.statusOf(item.getExpiryDate());
            if (status == FreshnessStatus.EXPIRED) {
                continue;
            }
            Long ingredientId = item.getIngredient().getId();
            available.merge(ingredientId, item.getQuantity(), Double::sum);
            if (status == FreshnessStatus.EXPIRING_SOON) {
                expiringSoon.add(ingredientId);
            }
        }
        return new FridgeStock(available, expiringSoon);
    }

    private static RecipeMatch match(Recipe recipe, FridgeStock stock) {
        List<IngredientRequirement> requirements = recipe.getIngredients().stream()
                .map(recipeIngredient -> requirement(recipeIngredient, stock))
                .toList();
        int expiringUsed = (int) requirements.stream().filter(IngredientRequirement::expiringSoon).count();
        return new RecipeMatch(
                recipe.getId(),
                recipe.getName(),
                recipe.getDescription(),
                recipe.getInstructions(),
                recipe.getCookingTimeMinutes(),
                matchPercent(requirements),
                expiringUsed,
                requirements);
    }

    private static IngredientRequirement requirement(RecipeIngredient recipeIngredient, FridgeStock stock) {
        Ingredient ingredient = recipeIngredient.getIngredient();
        Long id = ingredient.getId();
        return new IngredientRequirement(
                id,
                ingredient.getName(),
                ingredient.getUnit(),
                recipeIngredient.getQuantity(),
                stock.availableOf(id),
                stock.isExpiringSoon(id));
    }

    /** Usable (not expired) quantity per ingredient id. */
    private record FridgeStock(Map<Long, Double> available, Set<Long> expiringSoon) {

        double availableOf(Long ingredientId) {
            return available.getOrDefault(ingredientId, 0.0);
        }

        boolean isExpiringSoon(Long ingredientId) {
            return expiringSoon.contains(ingredientId);
        }
    }
}
