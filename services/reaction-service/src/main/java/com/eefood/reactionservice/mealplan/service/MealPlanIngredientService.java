package com.eefood.reactionservice.mealplan.service;

import com.eefood.reactionservice.dto.response.RecipeIngredientsResponse;
import com.eefood.reactionservice.mealplan.dto.request.MealPlanItemIngredientUpsertRequest;
import com.eefood.reactionservice.mealplan.dto.response.MealPlanItemIngredientResponse;
import com.eefood.reactionservice.mealplan.dto.response.MealPlanItemResponse;
import com.eefood.reactionservice.mealplan.mapper.MealPlanItemMapper;
import com.eefood.reactionservice.mealplan.model.MealPlanItem;
import com.eefood.reactionservice.mealplan.model.MealPlanItemIngredient;
import com.eefood.reactionservice.mealplan.repo.MealPlanItemIngredientRepository;
import com.eefood.reactionservice.mealplan.repo.MealPlanItemRepository;
import com.eefood.reactionservice.repository.httpclient.RecipeClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MealPlanIngredientService {

    private final MealPlanItemRepository mealPlanItemRepository;
    private final MealPlanItemIngredientRepository mealPlanItemIngredientRepository;
    private final MealPlanItemMapper mealPlanItemMapper;
    private final RecipeClient recipeClient;

    public void hydrateIngredients(List<MealPlanItemResponse> items) {
        if (items == null || items.isEmpty()) {
            return;
        }

        Map<Long, List<MealPlanItemIngredientResponse>> ingredientsByItemId = mealPlanItemIngredientRepository
                .findAllByMealPlanItemIdIn(items.stream().map(MealPlanItemResponse::getId).toList())
                .stream()
                .collect(Collectors.groupingBy(
                        MealPlanItemIngredient::getMealPlanItemId,
                        Collectors.mapping(mealPlanItemMapper::toResponse, Collectors.toList())
                ));

        items.forEach(item -> item.setIngredients(ingredientsByItemId.getOrDefault(item.getId(), List.of())));
    }

    public List<MealPlanItemIngredientResponse> getIngredientResponses(Long mealPlanItemId) {
        if (mealPlanItemId == null) {
            return List.of();
        }

        return mealPlanItemIngredientRepository.findAllByMealPlanItemIdIn(List.of(mealPlanItemId)).stream()
                .map(mealPlanItemMapper::toResponse)
                .toList();
    }

    public void replaceIngredients(Long mealPlanItemId, List<MealPlanItemIngredientUpsertRequest> ingredients) {
        mealPlanItemIngredientRepository.deleteAllByMealPlanItemId(mealPlanItemId);

        List<MealPlanItemIngredient> entities = ingredients.stream()
                .filter(Objects::nonNull)
                .filter(ingredient -> ingredient.getName() != null && !ingredient.getName().isBlank())
                .map(ingredient -> mealPlanItemMapper.toEntity(ingredient, mealPlanItemId))
                .toList();

        if (!entities.isEmpty()) {
            mealPlanItemIngredientRepository.saveAll(entities);
        }
    }

    public void replaceIngredientsFromRecipe(Long mealPlanItemId, Long recipeId) {
        // Lay nguyen lieu goc tu recipe-service de luu cho item.
        RecipeIngredientsResponse recipe = recipeClient.getRecipeIngredients(recipeId).getData();
        List<RecipeIngredientsResponse.RecipeIngredient> ingredients =
                recipe == null || recipe.getIngredients() == null
                        ? List.of()
                        : recipe.getIngredients();

        replaceIngredients(
                mealPlanItemId,
                ingredients.stream()
                        .filter(ingredient -> ingredient.getIngredient() != null)
                        .map(ingredient -> MealPlanItemIngredientUpsertRequest.builder()
                                .name(ingredient.getIngredient().getName())
                                .quantity(formatQuantity(ingredient.getQuantity()))
                                .unit(ingredient.getUnit())
                                .build())
                        .toList()
        );
    }

    public void deleteIngredientsByItemId(Long mealPlanItemId) {
        if (mealPlanItemId != null) {
            mealPlanItemIngredientRepository.deleteAllByMealPlanItemId(mealPlanItemId);
        }
    }

    public void deleteIngredientsByMealPlanId(Long mealPlanId) {
        List<Long> itemIds = mealPlanItemRepository.findAllByMealPlanIdOrderByPlanDateAscMealSlotAscItemOrderAsc(mealPlanId).stream()
                .map(MealPlanItem::getId)
                .toList();

        deleteIngredientsByItemIds(itemIds);
    }

    public void deleteIngredientsByMealPlanIdAndDateRange(Long mealPlanId, LocalDate startDate, LocalDate endDate) {
        List<Long> itemIds = mealPlanItemRepository.findAllByMealPlanIdOrderByPlanDateAscMealSlotAscItemOrderAsc(mealPlanId).stream()
                .filter(item -> item.getPlanDate() != null)
                .filter(item -> !item.getPlanDate().isBefore(startDate) && !item.getPlanDate().isAfter(endDate))
                .map(MealPlanItem::getId)
                .toList();

        deleteIngredientsByItemIds(itemIds);
    }

    private void deleteIngredientsByItemIds(Collection<Long> itemIds) {
        if (itemIds != null && !itemIds.isEmpty()) {
            mealPlanItemIngredientRepository.deleteAllByMealPlanItemIdIn(itemIds);
        }
    }

    private String formatQuantity(Double quantity) {
        // Chuyen so luong sang chuoi gon, vi custom item cho phep nhap tu do.
        return quantity == null
                ? null
                : BigDecimal.valueOf(quantity).stripTrailingZeros().toPlainString();
    }
}
