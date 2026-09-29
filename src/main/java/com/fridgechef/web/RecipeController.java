package com.fridgechef.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.fridgechef.dto.RecipeMatch;
import com.fridgechef.exception.InsufficientIngredientsException;
import com.fridgechef.service.CookingService;
import com.fridgechef.service.RecipeMatchingService;
import com.fridgechef.service.ShoppingListService;

@Controller
@RequestMapping("/recipes")
public class RecipeController {

    private static final String REDIRECT_TO_RECIPE = "redirect:/recipes/{id}";

    private final RecipeMatchingService recipeMatchingService;
    private final CookingService cookingService;
    private final ShoppingListService shoppingListService;

    public RecipeController(RecipeMatchingService recipeMatchingService,
                            CookingService cookingService,
                            ShoppingListService shoppingListService) {
        this.recipeMatchingService = recipeMatchingService;
        this.cookingService = cookingService;
        this.shoppingListService = shoppingListService;
    }

    @GetMapping
    public String list(@RequestParam(defaultValue = "0") int minMatch, Model model) {
        int threshold = Math.clamp(minMatch, 0, 100);
        model.addAttribute("minMatch", threshold);
        model.addAttribute("matches", recipeMatchingService.findMatches(threshold));
        return "recipes";
    }

    @GetMapping("/{id}")
    public String details(@PathVariable Long id, Model model) {
        model.addAttribute("match", recipeMatchingService.getMatch(id));
        return "recipe-details";
    }

    @PostMapping("/{id}/cook")
    public String cook(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            RecipeMatch cooked = cookingService.cook(id);
            FlashMessages.success(redirectAttributes,
                    "Enjoy your " + cooked.name() + "! The ingredients were taken from the fridge.");
        } catch (InsufficientIngredientsException ex) {
            FlashMessages.error(redirectAttributes,
                    "Not enough ingredients: " + String.join(", ", ex.getMissingIngredients()));
        }
        return REDIRECT_TO_RECIPE;
    }

    @PostMapping("/{id}/shopping-list")
    public String addMissingToShoppingList(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        int added = shoppingListService.addMissingIngredients(id);
        FlashMessages.success(redirectAttributes, added == 0
                ? "You already have everything for this recipe"
                : added + " missing ingredient(s) added to the shopping list");
        return REDIRECT_TO_RECIPE;
    }
}
