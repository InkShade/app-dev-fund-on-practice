package com.fridgechef.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fridgechef.TestData;
import com.fridgechef.domain.Ingredient;
import com.fridgechef.domain.MeasureUnit;
import com.fridgechef.dto.IngredientForm;
import com.fridgechef.dto.IngredientView;
import com.fridgechef.exception.DuplicateIngredientException;
import com.fridgechef.exception.ResourceNotFoundException;
import com.fridgechef.mapper.IngredientMapper;
import com.fridgechef.repository.IngredientRepository;

@ExtendWith(MockitoExtension.class)
class IngredientServiceTest {

    @Mock
    private IngredientRepository ingredientRepository;

    private IngredientService ingredientService;

    @BeforeEach
    void setUp() {
        ingredientService = new IngredientService(ingredientRepository, new IngredientMapper());
    }

    @Test
    void findAllReturnsViewsSortedByRepository() {
        when(ingredientRepository.findAllByOrderByNameAsc()).thenReturn(List.of(
                TestData.ingredient(1L, "Eggs", MeasureUnit.PIECE),
                TestData.ingredient(2L, "Milk", MeasureUnit.MILLILITER)));

        assertThat(ingredientService.findAll())
                .extracting(IngredientView::name)
                .containsExactly("Eggs", "Milk");
    }

    @Test
    void createSavesNewIngredient() {
        when(ingredientRepository.existsByNameIgnoreCase("Butter")).thenReturn(false);
        when(ingredientRepository.save(any(Ingredient.class)))
                .thenReturn(TestData.ingredient(7L, "Butter", MeasureUnit.GRAM, 30));

        IngredientView created = ingredientService.create(form(" Butter "));

        assertThat(created).isEqualTo(new IngredientView(7L, "Butter", MeasureUnit.GRAM, 30));
    }

    @Test
    void createRejectsDuplicateName() {
        when(ingredientRepository.existsByNameIgnoreCase("Butter")).thenReturn(true);
        IngredientForm duplicate = form("Butter");

        assertThatThrownBy(() -> ingredientService.create(duplicate))
                .isInstanceOf(DuplicateIngredientException.class)
                .hasMessageContaining("Butter");
        verify(ingredientRepository, never()).save(any());
    }

    @Test
    void getEntityThrowsWhenMissing() {
        when(ingredientRepository.findById(42L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ingredientService.getEntity(42L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Ingredient with id 42 was not found");
    }

    private static IngredientForm form(String name) {
        IngredientForm form = new IngredientForm();
        form.setName(name);
        form.setUnit(MeasureUnit.GRAM);
        form.setShelfLifeDays(30);
        return form;
    }
}
