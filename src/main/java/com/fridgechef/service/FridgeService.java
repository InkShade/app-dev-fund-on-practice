package com.fridgechef.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fridgechef.domain.FreshnessStatus;
import com.fridgechef.domain.FridgeItem;
import com.fridgechef.domain.Ingredient;
import com.fridgechef.dto.FridgeItemForm;
import com.fridgechef.dto.FridgeItemView;
import com.fridgechef.dto.FridgeSummary;
import com.fridgechef.exception.ResourceNotFoundException;
import com.fridgechef.mapper.FridgeItemMapper;
import com.fridgechef.repository.FridgeItemRepository;

@Service
@Transactional(readOnly = true)
public class FridgeService {

    private final FridgeItemRepository fridgeItemRepository;
    private final IngredientService ingredientService;
    private final FridgeItemMapper fridgeItemMapper;
    private final FreshnessPolicy freshnessPolicy;

    public FridgeService(FridgeItemRepository fridgeItemRepository,
                         IngredientService ingredientService,
                         FridgeItemMapper fridgeItemMapper,
                         FreshnessPolicy freshnessPolicy) {
        this.fridgeItemRepository = fridgeItemRepository;
        this.ingredientService = ingredientService;
        this.fridgeItemMapper = fridgeItemMapper;
        this.freshnessPolicy = freshnessPolicy;
    }

    /** All products in the fridge, the ones expiring first come first. */
    public List<FridgeItemView> findAll() {
        return fridgeItemRepository.findAllByOrderByExpiryDateAscIdAsc().stream()
                .map(fridgeItemMapper::toView)
                .toList();
    }

    public List<FridgeItemView> findExpiringSoon() {
        return findAll().stream()
                .filter(item -> item.status() == FreshnessStatus.EXPIRING_SOON)
                .toList();
    }

    public FridgeSummary summary() {
        List<FridgeItemView> items = findAll();
        return new FridgeSummary(
                items.size(),
                countByStatus(items, FreshnessStatus.EXPIRING_SOON),
                countByStatus(items, FreshnessStatus.EXPIRED));
    }

    @Transactional
    public FridgeItemView add(FridgeItemForm form) {
        Ingredient ingredient = ingredientService.getEntity(form.getIngredientId());
        LocalDate expiryDate = form.getExpiryDate() != null
                ? form.getExpiryDate()
                : defaultExpiryDate(ingredient);
        FridgeItem saved = fridgeItemRepository.save(new FridgeItem(ingredient, form.getQuantity(), expiryDate));
        return fridgeItemMapper.toView(saved);
    }

    /** Puts a freshly bought product into the fridge using its usual shelf life. */
    @Transactional
    public void addPurchased(Ingredient ingredient, double quantity) {
        fridgeItemRepository.save(new FridgeItem(ingredient, quantity, defaultExpiryDate(ingredient)));
    }

    @Transactional
    public void remove(Long id) {
        if (!fridgeItemRepository.existsById(id)) {
            throw new ResourceNotFoundException("Fridge item", id);
        }
        fridgeItemRepository.deleteById(id);
    }

    /** @return how many expired products were thrown away */
    @Transactional
    public long discardExpired() {
        return fridgeItemRepository.deleteByExpiryDateBefore(freshnessPolicy.today());
    }

    private LocalDate defaultExpiryDate(Ingredient ingredient) {
        return freshnessPolicy.today().plusDays(ingredient.getShelfLifeDays());
    }

    private static int countByStatus(List<FridgeItemView> items, FreshnessStatus status) {
        return (int) items.stream().filter(item -> item.status() == status).count();
    }
}
