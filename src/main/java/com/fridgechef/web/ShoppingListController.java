package com.fridgechef.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.fridgechef.dto.ShoppingItemForm;
import com.fridgechef.service.IngredientService;
import com.fridgechef.service.ShoppingListService;

import jakarta.validation.Valid;

@Controller
@RequestMapping("/shopping-list")
public class ShoppingListController {

    private static final String VIEW = "shopping-list";
    private static final String REDIRECT = "redirect:/shopping-list";

    private final ShoppingListService shoppingListService;
    private final IngredientService ingredientService;

    public ShoppingListController(ShoppingListService shoppingListService, IngredientService ingredientService) {
        this.shoppingListService = shoppingListService;
        this.ingredientService = ingredientService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("shoppingItemForm", new ShoppingItemForm());
        populate(model);
        return VIEW;
    }

    @PostMapping
    public String add(@Valid @ModelAttribute("shoppingItemForm") ShoppingItemForm form,
                      BindingResult bindingResult,
                      Model model,
                      RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            populate(model);
            return VIEW;
        }
        shoppingListService.add(form);
        FlashMessages.success(redirectAttributes, "Added to the shopping list");
        return REDIRECT;
    }

    @PostMapping("/{id}/toggle")
    public String togglePurchased(@PathVariable Long id) {
        shoppingListService.togglePurchased(id);
        return REDIRECT;
    }

    @PostMapping("/{id}/delete")
    public String remove(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        shoppingListService.remove(id);
        FlashMessages.success(redirectAttributes, "Removed from the shopping list");
        return REDIRECT;
    }

    @PostMapping("/move-to-fridge")
    public String movePurchasedToFridge(RedirectAttributes redirectAttributes) {
        int moved = shoppingListService.movePurchasedToFridge();
        if (moved == 0) {
            FlashMessages.error(redirectAttributes, "Tick the products you have bought first");
        } else {
            FlashMessages.success(redirectAttributes, moved + " product(s) moved into the fridge");
        }
        return REDIRECT;
    }

    private void populate(Model model) {
        model.addAttribute("items", shoppingListService.findAll());
        model.addAttribute("ingredients", ingredientService.findAll());
    }
}
