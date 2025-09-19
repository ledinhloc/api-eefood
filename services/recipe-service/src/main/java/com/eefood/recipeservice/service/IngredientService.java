package com.eefood.recipeservice.service;

import com.eefood.recipeservice.dto.response.IngredientResponse;
import com.eefood.recipeservice.mapper.RecipeMapper;
import com.eefood.recipeservice.model.Ingredient;
import com.eefood.recipeservice.repository.IngredientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class IngredientService {
    private final IngredientRepository ingredientRepository;
    private final RecipeMapper recipeMapper;

    public Page<IngredientResponse> getAllIngredients(String name, Pageable pageable) {
        if (name == null || name.isBlank()) {
            return ingredientRepository.findAll(pageable)
                    .map(recipeMapper::toResponse);
        }
        Specification<Ingredient> spec = Specification.allOf(IngredientSpecification.hasName(name.trim()));
        return ingredientRepository.findAll(spec, pageable).map(recipeMapper::toResponse);
    }
}
