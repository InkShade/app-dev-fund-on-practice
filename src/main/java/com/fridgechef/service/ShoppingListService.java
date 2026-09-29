package com.fridgechef.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fridgechef.domain.Ingredient;
import com.fridgechef.domain.ShoppingListItem;
import com.fridgechef.dto.IngredientRequirement;
import com.fridgechef.dto.ShoppingItemForm;
import com.fridgechef.dto.ShoppingListItemView;
import com.fridgechef.exception.ResourceNotFoundException;
import com.fridgechef.mapper.ShoppingListItemMapper;
import com.fridgechef.repository.ShoppingListItemRepository;

@Service
@Transactional(readOnly = true)
public class ShoppingListService {

    private final ShoppingListItemRepository shoppingListItemRepository;
    private final IngredientService ingredientService;
    private final RecipeMatchingService recipeMatchingService;
    private final FridgeService fridgeService;
    private final ShoppingListItemMapper shoppingListItemMapper;

    public ShoppingListService(ShoppingListItemRepository shoppingListItemRepository,
                               IngredientService ingredientService,
                               RecipeMatchingService recipeMatchingService,
                               FridgeService fridgeService,
                               ShoppingListItemMapper shoppingListItemMapper) {
        this.shoppingListItemRepository = shoppingListItemRepository;
        this.ingredientService = ingredientService;
        this.recipeMatchingService = recipeMatchingService;
        this.fridgeService = fridgeService;
        this.shoppingListItemMapper = shoppingListItemMapper;
    }

    /** Items still to buy come first, purchased ones last. */
    public List<ShoppingListItemView> findAll() {
        return shoppingListItemRepository.findAllByOrderByPurchasedAscIdAsc().stream()
                .map(shoppingListItemMapper::toView)
                .toList();
    }

    @Transactional
    public void add(ShoppingItemForm form) {
        addOrIncrease(ingredientService.getEntity(form.getIngredientId()), form.getQuantity());
    }

    /**
     * Puts everything the recipe still lacks on the shopping list.
     *
     * @return how many ingredients were added
     */
    @Transactional
    public int addMissingIngredients(Long recipeId) {
        List<IngredientRequirement> missing = recipeMatchingService.getMatch(recipeId).missingIngredients();
        for (IngredientRequirement requirement : missing) {
            addOrIncrease(ingredientService.getEntity(requirement.ingredientId()), requirement.missingQuantity());
        }
        return missing.size();
    }

    @Transactional
    public void togglePurchased(Long id) {
        getEntity(id).togglePurchased();
    }

    @Transactional
    public void remove(Long id) {
        shoppingListItemRepository.delete(getEntity(id));
    }

    /**
     * Moves purchased items into the fridge and removes them from the list.
     *
     * @return how many products were put into the fridge
     */
    @Transactional
    public int movePurchasedToFridge() {
        List<ShoppingListItem> purchased = shoppingListItemRepository.findByPurchasedTrue();
        for (ShoppingListItem item : purchased) {
            fridgeService.addPurchased(item.getIngredient(), item.getQuantity());
        }
        shoppingListItemRepository.deleteAll(purchased);
        return purchased.size();
    }

    /** An ingredient appears on the list at most once while it is not bought yet. */
    private void addOrIncrease(Ingredient ingredient, double quantity) {
        shoppingListItemRepository.findByIngredientIdAndPurchasedFalse(ingredient.getId())
                .ifPresentOrElse(
                        item -> item.increaseQuantity(quantity),
                        () -> shoppingListItemRepository.save(new ShoppingListItem(ingredient, quantity)));
    }

    private ShoppingListItem getEntity(Long id) {
        return shoppingListItemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Shopping list item", id));
    }
}
