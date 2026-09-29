package com.fridgechef.service;

import static com.fridgechef.TestData.TODAY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fridgechef.TestData;
import com.fridgechef.domain.FreshnessStatus;
import com.fridgechef.domain.FridgeItem;
import com.fridgechef.domain.Ingredient;
import com.fridgechef.domain.MeasureUnit;
import com.fridgechef.dto.FridgeItemForm;
import com.fridgechef.dto.FridgeItemView;
import com.fridgechef.dto.FridgeSummary;
import com.fridgechef.exception.ResourceNotFoundException;
import com.fridgechef.mapper.FridgeItemMapper;
import com.fridgechef.repository.FridgeItemRepository;

@ExtendWith(MockitoExtension.class)
class FridgeServiceTest {

    @Mock
    private FridgeItemRepository fridgeItemRepository;

    @Mock
    private IngredientService ingredientService;

    private FridgeService fridgeService;

    private final Ingredient milk = TestData.ingredient(1L, "Milk", MeasureUnit.MILLILITER, 7);
    private final Ingredient eggs = TestData.ingredient(2L, "Eggs", MeasureUnit.PIECE, 21);

    @BeforeEach
    void setUp() {
        FreshnessPolicy policy = TestData.freshnessPolicy();
        fridgeService = new FridgeService(fridgeItemRepository, ingredientService, new FridgeItemMapper(policy), policy);
    }

    @Test
    void findAllMapsItemsWithFreshness() {
        givenFridgeContains(
                TestData.fridgeItem(1L, milk, 300, TODAY.minusDays(1)),
                TestData.fridgeItem(2L, eggs, 6, TODAY.plusDays(10)));

        assertThat(fridgeService.findAll())
                .extracting(FridgeItemView::ingredientName, FridgeItemView::status)
                .containsExactly(
                        tuple("Milk", FreshnessStatus.EXPIRED),
                        tuple("Eggs", FreshnessStatus.FRESH));
    }

    @Test
    void findExpiringSoonReturnsOnlyItemsAboutToExpire() {
        givenFridgeContains(
                TestData.fridgeItem(1L, milk, 300, TODAY.minusDays(1)),
                TestData.fridgeItem(2L, milk, 500, TODAY.plusDays(2)),
                TestData.fridgeItem(3L, eggs, 6, TODAY.plusDays(10)));

        assertThat(fridgeService.findExpiringSoon())
                .extracting(FridgeItemView::id)
                .containsExactly(2L);
    }

    @Test
    void summaryCountsItemsByStatus() {
        givenFridgeContains(
                TestData.fridgeItem(1L, milk, 300, TODAY.minusDays(1)),
                TestData.fridgeItem(2L, milk, 500, TODAY),
                TestData.fridgeItem(3L, eggs, 6, TODAY.plusDays(1)),
                TestData.fridgeItem(4L, eggs, 6, TODAY.plusDays(10)));

        assertThat(fridgeService.summary()).isEqualTo(new FridgeSummary(4, 2, 1));
    }

    @Test
    void addUsesGivenExpiryDate() {
        when(ingredientService.getEntity(1L)).thenReturn(milk);
        when(fridgeItemRepository.save(any(FridgeItem.class))).thenAnswer(call -> call.getArgument(0));

        FridgeItemView added = fridgeService.add(form(1L, 250.0, TODAY.plusDays(4)));

        assertThat(added.quantity()).isEqualTo(250);
        assertThat(added.expiryDate()).isEqualTo(TODAY.plusDays(4));
        assertThat(added.status()).isEqualTo(FreshnessStatus.FRESH);
    }

    @Test
    void addFallsBackToIngredientShelfLife() {
        when(ingredientService.getEntity(2L)).thenReturn(eggs);
        when(fridgeItemRepository.save(any(FridgeItem.class))).thenAnswer(call -> call.getArgument(0));

        FridgeItemView added = fridgeService.add(form(2L, 10.0, null));

        assertThat(added.expiryDate()).isEqualTo(TODAY.plusDays(21));
    }

    @Test
    void addPurchasedStoresProductWithDefaultShelfLife() {
        fridgeService.addPurchased(milk, 1000);

        ArgumentCaptor<FridgeItem> saved = ArgumentCaptor.forClass(FridgeItem.class);
        verify(fridgeItemRepository).save(saved.capture());
        assertThat(saved.getValue().getIngredient()).isSameAs(milk);
        assertThat(saved.getValue().getQuantity()).isEqualTo(1000);
        assertThat(saved.getValue().getExpiryDate()).isEqualTo(TODAY.plusDays(7));
    }

    @Test
    void removeDeletesExistingItem() {
        when(fridgeItemRepository.existsById(5L)).thenReturn(true);

        fridgeService.remove(5L);

        verify(fridgeItemRepository).deleteById(5L);
    }

    @Test
    void removeThrowsForUnknownItem() {
        when(fridgeItemRepository.existsById(5L)).thenReturn(false);

        assertThatThrownBy(() -> fridgeService.remove(5L)).isInstanceOf(ResourceNotFoundException.class);
        verify(fridgeItemRepository, never()).deleteById(any());
    }

    @Test
    void discardExpiredDeletesItemsExpiredBeforeToday() {
        when(fridgeItemRepository.deleteByExpiryDateBefore(TODAY)).thenReturn(3L);

        assertThat(fridgeService.discardExpired()).isEqualTo(3);
    }

    private void givenFridgeContains(FridgeItem... items) {
        when(fridgeItemRepository.findAllByOrderByExpiryDateAscIdAsc()).thenReturn(List.of(items));
    }

    private static FridgeItemForm form(Long ingredientId, Double quantity, LocalDate expiryDate) {
        FridgeItemForm form = new FridgeItemForm();
        form.setIngredientId(ingredientId);
        form.setQuantity(quantity);
        form.setExpiryDate(expiryDate);
        return form;
    }
}
