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

import com.fridgechef.domain.MeasureUnit;
import com.fridgechef.dto.IngredientForm;
import com.fridgechef.dto.IngredientOverview;
import com.fridgechef.dto.IngredientView;
import com.fridgechef.exception.DuplicateIngredientException;
import com.fridgechef.exception.IngredientInUseException;
import com.fridgechef.service.IngredientService;

import jakarta.validation.Valid;

@Controller
@RequestMapping("/ingredients")
public class IngredientController {

    private static final String VIEW = "ingredients";
    private static final String EDIT_VIEW = "ingredient-edit";
    private static final String INGREDIENTS_ATTRIBUTE = "ingredients";
    private static final String OVERVIEW_ATTRIBUTE = "overview";
    private static final String FORM_ATTRIBUTE = "ingredientForm";
    private static final String REDIRECT_TO_LIST = "redirect:/ingredients";

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
        model.addAttribute(FORM_ATTRIBUTE, new IngredientForm());
        model.addAttribute(INGREDIENTS_ATTRIBUTE, ingredientService.findAllWithUsage());
        return VIEW;
    }

    @PostMapping
    public String create(@Valid @ModelAttribute(FORM_ATTRIBUTE) IngredientForm form,
                         BindingResult bindingResult,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        if (!bindingResult.hasErrors()) {
            try {
                IngredientView created = ingredientService.create(form);
                FlashMessages.success(redirectAttributes, created.name() + " was added to the catalogue");
                return REDIRECT_TO_LIST;
            } catch (DuplicateIngredientException ex) {
                bindingResult.rejectValue("name", "duplicate", ex.getMessage());
            }
        }
        model.addAttribute(INGREDIENTS_ATTRIBUTE, ingredientService.findAllWithUsage());
        return VIEW;
    }

    @GetMapping("/{id}/edit")
    public String edit(@PathVariable Long id, Model model) {
        IngredientOverview overview = ingredientService.getOverview(id);
        model.addAttribute(OVERVIEW_ATTRIBUTE, overview);
        model.addAttribute(FORM_ATTRIBUTE, IngredientForm.from(overview.ingredient()));
        return EDIT_VIEW;
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute(FORM_ATTRIBUTE) IngredientForm form,
                         BindingResult bindingResult,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        if (!bindingResult.hasErrors()) {
            try {
                IngredientView updated = ingredientService.update(id, form);
                FlashMessages.success(redirectAttributes, updated.name() + " was updated");
                return REDIRECT_TO_LIST;
            } catch (DuplicateIngredientException ex) {
                bindingResult.rejectValue("name", "duplicate", ex.getMessage());
            } catch (IngredientInUseException ex) {
                bindingResult.rejectValue("unit", "inUse", ex.getMessage());
            }
        }
        model.addAttribute(OVERVIEW_ATTRIBUTE, ingredientService.getOverview(id));
        return EDIT_VIEW;
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            String name = ingredientService.delete(id);
            FlashMessages.success(redirectAttributes, name + " was removed from the catalogue");
        } catch (IngredientInUseException ex) {
            FlashMessages.error(redirectAttributes, ex.getMessage());
        }
        return REDIRECT_TO_LIST;
    }
}
