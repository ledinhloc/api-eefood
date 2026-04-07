package com.eefood.recipeservice.service;

import com.eefood.recipeservice.dto.response.IngredientAlterResponse;
import com.eefood.recipeservice.enums.ErrorMessage;
import com.eefood.recipeservice.exception.ExceptionUtil;
import com.eefood.recipeservice.mapper.RecipeMapper;
import com.eefood.recipeservice.model.Ingredient;
import com.eefood.recipeservice.model.Recipe;
import com.eefood.recipeservice.model.RecipeIngredient;
import com.eefood.recipeservice.repository.IngredientRepository;
import com.eefood.recipeservice.repository.IngredientSubstituteRepository;
import com.eefood.recipeservice.repository.RecipeIngredientRepository;
import com.eefood.recipeservice.repository.RecipeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AlternateIngredientService {
    private final RecipeRepository recipeRepository;
    private final RecipeIngredientRepository recipeIngredientRepository;
    private final IngredientSubstituteRepository ingredientSubstituteRepository;
    private final IngredientRepository ingredientRepository;
    private final RecipeMapper recipeMapper;
    public List<IngredientAlterResponse> getIngredientAndSub(Long recipeId) {
        Recipe recipe = recipeRepository.findByIdAndIsDeletedFalse(recipeId)
                .orElseThrow(() -> ExceptionUtil.notFound(ErrorMessage.RECIPE_NOT_FOUND));

        List<RecipeIngredient> recipeIngredients = recipe.getIngredients().stream().toList();

        if (recipeIngredients.isEmpty()) {
            return List.of();
        }

        return recipeIngredients.stream()
                .map(ri -> {
                    Ingredient ingredient = ri.getIngredient();

                    List<Ingredient> substitutes =
                            ingredientSubstituteRepository.findSubstitutesByIngredientId(ingredient.getId());

                    if (substitutes.isEmpty()) {
                        return null;
                    }

                    return IngredientAlterResponse.builder()
                            .ingredient(recipeMapper.toResponse(ingredient))
                            .substitute(
                                    substitutes.stream()
                                            .map(recipeMapper::toResponse)
                                            .toList()
                            )
                            .build();
                })
                .filter(Objects::nonNull)
                .toList();
    }
}
