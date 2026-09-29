package com.fridgechef.service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fridgechef.domain.Ingredient;
import com.fridgechef.dto.IngredientForm;
import com.fridgechef.dto.IngredientOverview;
import com.fridgechef.dto.IngredientUsage;
import com.fridgechef.dto.IngredientView;
import com.fridgechef.exception.DuplicateIngredientException;
import com.fridgechef.exception.IngredientInUseException;
import com.fridgechef.exception.ResourceNotFoundException;
import com.fridgechef.mapper.IngredientMapper;
import com.fridgechef.repository.FridgeItemRepository;
import com.fridgechef.repository.IngredientCount;
import com.fridgechef.repository.IngredientRepository;
import com.fridgechef.repository.RecipeRepository;
import com.fridgechef.repository.ShoppingListItemRepository;

/**
 * Ingredient catalogue. Fridge items, recipes and the shopping list reference ingredients by id, so renaming
 * is always safe. Deleting an ingredient or changing its unit is only allowed while nothing uses it:
 * otherwise recipes would lose ingredients and stored quantities would silently change meaning (200 g to 200 ml).
 */
@Service
@Transactional(readOnly = true)
public class IngredientService {

    private final IngredientRepository ingredientRepository;
    private final RecipeRepository recipeRepository;
    private final FridgeItemRepository fridgeItemRepository;
    private final ShoppingListItemRepository shoppingListItemRepository;
    private final IngredientMapper ingredientMapper;

    public IngredientService(IngredientRepository ingredientRepository,
                             RecipeRepository recipeRepository,
                             FridgeItemRepository fridgeItemRepository,
                             ShoppingListItemRepository shoppingListItemRepository,
                             IngredientMapper ingredientMapper) {
        this.ingredientRepository = ingredientRepository;
        this.recipeRepository = recipeRepository;
        this.fridgeItemRepository = fridgeItemRepository;
        this.shoppingListItemRepository = shoppingListItemRepository;
        this.ingredientMapper = ingredientMapper;
    }

    public List<IngredientView> findAll() {
        return ingredientRepository.findAllByOrderByNameAsc().stream()
                .map(ingredientMapper::toView)
                .toList();
    }

    /** The whole catalogue with usage counts, loaded with one query per referencing table. */
    public List<IngredientOverview> findAllWithUsage() {
        Map<Long, Long> recipes = toMap(recipeRepository.countPerIngredient());
        Map<Long, Long> fridgeItems = toMap(fridgeItemRepository.countPerIngredient());
        Map<Long, Long> shoppingListItems = toMap(shoppingListItemRepository.countPerIngredient());
        return ingredientRepository.findAllByOrderByNameAsc().stream()
                .map(ingredient -> new IngredientOverview(
                        ingredientMapper.toView(ingredient),
                        new IngredientUsage(
                                recipes.getOrDefault(ingredient.getId(), 0L),
                                fridgeItems.getOrDefault(ingredient.getId(), 0L),
                                shoppingListItems.getOrDefault(ingredient.getId(), 0L))))
                .toList();
    }

    public IngredientOverview getOverview(Long id) {
        Ingredient ingredient = getEntity(id);
        return new IngredientOverview(ingredientMapper.toView(ingredient), usageOf(id));
    }

    @Transactional
    public IngredientView create(IngredientForm form) {
        String name = form.getName().strip();
        if (ingredientRepository.existsByNameIgnoreCase(name)) {
            throw new DuplicateIngredientException(name);
        }
        Ingredient saved = ingredientRepository.save(ingredientMapper.toEntity(form));
        return ingredientMapper.toView(saved);
    }

    /**
     * Renames the ingredient and updates its shelf life. The new shelf life only applies to products added
     * later; best-before dates already in the fridge stay as they are.
     */
    @Transactional
    public IngredientView update(Long id, IngredientForm form) {
        Ingredient ingredient = getEntity(id);
        String name = form.getName().strip();
        if (ingredientRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw new DuplicateIngredientException(name);
        }
        if (form.getUnit() != ingredient.getUnit()) {
            IngredientUsage usage = usageOf(id);
            if (usage.isUsed()) {
                throw new IngredientInUseException("change the unit of", ingredient.getName(), usage.summary());
            }
        }
        ingredient.update(name, form.getUnit(), form.getShelfLifeDays());
        return ingredientMapper.toView(ingredient);
    }

    /** Deletes an unused ingredient and returns its name. */
    @Transactional
    public String delete(Long id) {
        Ingredient ingredient = getEntity(id);
        IngredientUsage usage = usageOf(id);
        if (usage.isUsed()) {
            throw new IngredientInUseException("delete", ingredient.getName(), usage.summary());
        }
        ingredientRepository.delete(ingredient);
        return ingredient.getName();
    }

    Ingredient getEntity(Long id) {
        return ingredientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ingredient", id));
    }

    private IngredientUsage usageOf(Long id) {
        return new IngredientUsage(
                recipeRepository.countUsingIngredient(id),
                fridgeItemRepository.countByIngredientId(id),
                shoppingListItemRepository.countByIngredientId(id));
    }

    private static Map<Long, Long> toMap(List<IngredientCount> counts) {
        return counts.stream().collect(Collectors.toMap(IngredientCount::getIngredientId, IngredientCount::getTotal));
    }
}
