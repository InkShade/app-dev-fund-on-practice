package com.fridgechef.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.fridgechef.service.FridgeService;
import com.fridgechef.service.RecipeMatchingService;

@Controller
public class HomeController {

    static final int TOP_MATCHES = 3;

    private final FridgeService fridgeService;
    private final RecipeMatchingService recipeMatchingService;

    public HomeController(FridgeService fridgeService, RecipeMatchingService recipeMatchingService) {
        this.fridgeService = fridgeService;
        this.recipeMatchingService = recipeMatchingService;
    }

    @GetMapping("/")
    public String dashboard(Model model) {
        model.addAttribute("summary", fridgeService.summary());
        model.addAttribute("expiringItems", fridgeService.findExpiringSoon());
        model.addAttribute("topMatches", recipeMatchingService.findMatches(1).stream().limit(TOP_MATCHES).toList());
        return "index";
    }
}
