package com.eefood.recipeservice.service;

import com.eefood.recipeservice.dto.response.IngredientAlterResponse;
import com.eefood.recipeservice.dto.response.IngredientDetailResponse;
import com.eefood.recipeservice.enums.ErrorMessage;
import com.eefood.recipeservice.exception.ExceptionUtil;
import com.eefood.recipeservice.mapper.RecipeMapper;
import com.eefood.recipeservice.model.Ingredient;
import com.eefood.recipeservice.model.Recipe;
import com.eefood.recipeservice.model.RecipeIngredient;
import com.eefood.recipeservice.model.UserIngredientSubstitution;
import com.eefood.recipeservice.repository.*;
import com.eefood.recipeservice.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
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
    private final UserIngredientSubstitutionRepository userIngreSubRepository;
    private final RecipeMapper recipeMapper;
    private final SecurityUtil securityUtil;

    public IngredientAlterResponse getIngredientAlterResponse(Long recipeId, Long ingredientId) {
        Long userId = securityUtil.getCurrentUserId();

        RecipeIngredient ri = recipeIngredientRepository
                .findByRecipeIdAndIngredientIdAndIsDeletedFalse(recipeId, ingredientId)
                .orElse(null);
        if (ri == null) {
            throw ExceptionUtil.badRequest(ErrorMessage.RECIPE_NOT_FOUND);
        }

        Map<Long, UserIngredientSubstitution> userSubMap =
                userIngreSubRepository
                        .findByUserIdAndRecipeIngredient_Recipe_Id(userId, recipeId)
                        .stream()
                        .collect(Collectors.toMap(
                                s -> s.getRecipeIngredient().getId(),
                                s -> s
                        ));
        Ingredient ingredient = ri.getIngredient();
        log.info("Ingredient id: {}", ingredient.getId());

        List<Ingredient> substitutes =
                ingredientSubstituteRepository.findSubstitutesByIngredientId(ingredient.getId());
        log.info("List substitutes: {}", substitutes);
        if (substitutes.isEmpty()) {
            return null;
        }

        UserIngredientSubstitution userSub = userSubMap.get(ri.getId());
        IngredientDetailResponse selected = userSub != null
                ? recipeMapper.toDetailResponse(userSub.getSubstituteIngredient())
                : null;

        return IngredientAlterResponse.builder()
                .ingredient(recipeMapper.toDetailResponse(ingredient))
                .selectedSubstitute(selected)
                .substitute(
                        substitutes.stream()
                                .map(recipeMapper::toDetailResponse)
                                .toList()
                )
                .build();
    }

    public List<IngredientAlterResponse> getIngredientAndSub(Long recipeId) {

        Long userId = securityUtil.getCurrentUserId();

        Recipe recipe = recipeRepository.findByIdAndIsDeletedFalse(recipeId)
                .orElseThrow(() -> ExceptionUtil.notFound(ErrorMessage.RECIPE_NOT_FOUND));

        List<RecipeIngredient> recipeIngredients = recipe.getIngredients().stream().toList();

        if (recipeIngredients.isEmpty()) {
            return List.of();
        }

        Map<Long, UserIngredientSubstitution> userSubMap =
                userIngreSubRepository
                        .findByUserIdAndRecipeIngredient_Recipe_Id(userId, recipeId)
                        .stream()
                        .collect(Collectors.toMap(
                                s -> s.getRecipeIngredient().getId(),
                                s -> s
                        ));

        return recipeIngredients.stream()
                .map(ri -> {
                    Ingredient ingredient = ri.getIngredient();

                    List<Ingredient> substitutes =
                            ingredientSubstituteRepository.findSubstitutesByIngredientId(ingredient.getId());

                    if (substitutes.isEmpty()) {
                        return null;
                    }

                    UserIngredientSubstitution userSub = userSubMap.get(ri.getId());
                    IngredientDetailResponse selected = userSub != null
                            ? recipeMapper.toDetailResponse(userSub.getSubstituteIngredient())
                            : null;

                    return IngredientAlterResponse.builder()
                            .ingredient(recipeMapper.toDetailResponse(ingredient))
                            .selectedSubstitute(selected)
                            .substitute(
                                    substitutes.stream()
                                            .map(recipeMapper::toDetailResponse)
                                            .toList()
                            )
                            .build();
                })
                .filter(Objects::nonNull)
                .toList();
    }

    @Transactional
    public void selectAlterIngredient(Long recipeId, Long ingredientId, Long substituteId) {
        Long userId = securityUtil.getCurrentUserId();

        if(ingredientId.equals(substituteId)) {
            return;
        }
        RecipeIngredient ri = recipeIngredientRepository
                .findByRecipeIdAndIngredientIdAndIsDeletedFalse(recipeId,ingredientId)
                .orElseThrow(() -> ExceptionUtil.notFound(ErrorMessage.RECIPE_NOT_FOUND));

        Optional<UserIngredientSubstitution> existing =
                userIngreSubRepository.findByUserIdAndRecipeIngredientId(userId, ri.getId());

        if (substituteId == null) {
            existing.ifPresent(userIngreSubRepository::delete);
            return;
        }

        boolean isValidSubstitute = ingredientSubstituteRepository
                .existsByIngredientIdAndSubstituteId(ingredientId, substituteId);

        if (!isValidSubstitute) {
            throw ExceptionUtil.badRequest(ErrorMessage.SUBSTITUTE_NOT_FOUND);
        }

        Ingredient substitute = ingredientRepository.findById(substituteId)
                .orElseThrow(() -> ExceptionUtil.notFound(ErrorMessage.SUBSTITUTE_NOT_FOUND));

        if (existing.isPresent()) {
            existing.get().setSubstituteIngredient(substitute);
            userIngreSubRepository.save(existing.get());
        } else {
            userIngreSubRepository.save(
                    UserIngredientSubstitution.builder()
                            .recipeIngredient(ri)
                            .substituteIngredient(substitute)
                            .userId(userId)
                            .build()
            );
        }
    }
}
