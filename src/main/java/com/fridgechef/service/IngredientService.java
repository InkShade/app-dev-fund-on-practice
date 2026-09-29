package com.fridgechef.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fridgechef.domain.Ingredient;
import com.fridgechef.dto.IngredientForm;
import com.fridgechef.dto.IngredientView;
import com.fridgechef.exception.DuplicateIngredientException;
import com.fridgechef.exception.ResourceNotFoundException;
import com.fridgechef.mapper.IngredientMapper;
import com.fridgechef.repository.IngredientRepository;

@Service
@Transactional(readOnly = true)
public class IngredientService {

    private final IngredientRepository ingredientRepository;
    private final IngredientMapper ingredientMapper;

    public IngredientService(IngredientRepository ingredientRepository, IngredientMapper ingredientMapper) {
        this.ingredientRepository = ingredientRepository;
        this.ingredientMapper = ingredientMapper;
    }

    public List<IngredientView> findAll() {
        return ingredientRepository.findAllByOrderByNameAsc().stream()
                .map(ingredientMapper::toView)
                .toList();
    }

    @Transactional
    public IngredientView create(IngredientForm form) {
        String name = form.getName().strip();
        if (ingredientRepository.existsByNameIgnoreCase(name)) {
            throw new DuplicateIngredientException(name);
        }
        Ingredient saved = ingredientRepository.save(ingredientMapper.toEntity(form));
        return ingredientMapper.toView(saved);
    }

    Ingredient getEntity(Long id) {
        return ingredientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ingredient", id));
    }
}
