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

import com.fridgechef.dto.FridgeItemForm;
import com.fridgechef.dto.FridgeItemView;
import com.fridgechef.service.FridgeService;
import com.fridgechef.service.IngredientService;

import jakarta.validation.Valid;

@Controller
@RequestMapping("/fridge")
public class FridgeController {

    private static final String VIEW = "fridge";
    private static final String REDIRECT = "redirect:/fridge";

    private final FridgeService fridgeService;
    private final IngredientService ingredientService;

    public FridgeController(FridgeService fridgeService, IngredientService ingredientService) {
        this.fridgeService = fridgeService;
        this.ingredientService = ingredientService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("fridgeItemForm", new FridgeItemForm());
        populate(model);
        return VIEW;
    }

    @PostMapping
    public String add(@Valid @ModelAttribute("fridgeItemForm") FridgeItemForm form,
                      BindingResult bindingResult,
                      Model model,
                      RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            populate(model);
            return VIEW;
        }
        FridgeItemView added = fridgeService.add(form);
        FlashMessages.success(redirectAttributes,
                added.quantityLabel() + " of " + added.ingredientName() + " put into the fridge");
        return REDIRECT;
    }

    @PostMapping("/{id}/delete")
    public String remove(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        fridgeService.remove(id);
        FlashMessages.success(redirectAttributes, "Product removed from the fridge");
        return REDIRECT;
    }

    @PostMapping("/discard-expired")
    public String discardExpired(RedirectAttributes redirectAttributes) {
        long discarded = fridgeService.discardExpired();
        FlashMessages.success(redirectAttributes, discarded == 0
                ? "Nothing to throw away, everything is still good"
                : "Threw away " + discarded + " expired product(s)");
        return REDIRECT;
    }

    private void populate(Model model) {
        model.addAttribute("items", fridgeService.findAll());
        model.addAttribute("ingredients", ingredientService.findAll());
    }
}
