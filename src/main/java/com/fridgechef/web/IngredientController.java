package com.fridgechef.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.fridgechef.domain.MeasureUnit;
import com.fridgechef.dto.IngredientForm;
import com.fridgechef.dto.IngredientView;
import com.fridgechef.exception.DuplicateIngredientException;
import com.fridgechef.service.IngredientService;

import jakarta.validation.Valid;

@Controller
@RequestMapping("/ingredients")
public class IngredientController {

    private static final String VIEW = "ingredients";

    private final IngredientService ingredientService;

    public IngredientController(IngredientService ingredientService) {
        this.ingredientService = ingredientService;
    }

    @ModelAttribute("units")
    public MeasureUnit[] units() {
        return MeasureUnit.values();
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("ingredientForm", new IngredientForm());
        model.addAttribute("ingredients", ingredientService.findAll());
        return VIEW;
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("ingredientForm") IngredientForm form,
                         BindingResult bindingResult,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        if (!bindingResult.hasErrors()) {
            try {
                IngredientView created = ingredientService.create(form);
                FlashMessages.success(redirectAttributes, created.name() + " was added to the catalogue");
                return "redirect:/ingredients";
            } catch (DuplicateIngredientException ex) {
                bindingResult.rejectValue("name", "duplicate", ex.getMessage());
            }
        }
        model.addAttribute("ingredients", ingredientService.findAll());
        return VIEW;
    }
}
