package com.eefood.reactionservice.mealplan.mapper;

import com.eefood.reactionservice.mealplan.dto.ai.GeneratedMealItem;
import com.eefood.reactionservice.mealplan.dto.request.MealPlanItemIngredientUpsertRequest;
import com.eefood.reactionservice.mealplan.dto.response.MealPlanItemIngredientResponse;
import com.eefood.reactionservice.mealplan.dto.response.MealPlanItemResponse;
import com.eefood.reactionservice.mealplan.enums.MealPlanItemSource;
import com.eefood.reactionservice.mealplan.enums.MealPlanItemStatus;
import com.eefood.reactionservice.mealplan.model.MealPlanItem;
import com.eefood.reactionservice.mealplan.model.MealPlanItemIngredient;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.math.BigDecimal;

@Mapper(componentModel = "spring", imports = {MealPlanItemSource.class, MealPlanItemStatus.class})
public interface MealPlanItemMapper {
    @Mapping(target = "ingredients", ignore = true)
    MealPlanItemResponse toResponse(MealPlanItem mealPlanItem);

    @Mapping(target = "ingredients", ignore = true)
    @Mapping(target = "calories", expression = "java(scale(mealPlanItem.getCalories(), mealPlanItem))")
    @Mapping(target = "protein", expression = "java(scale(mealPlanItem.getProtein(), mealPlanItem))")
    @Mapping(target = "carbs", expression = "java(scale(mealPlanItem.getCarbs(), mealPlanItem))")
    @Mapping(target = "fat", expression = "java(scale(mealPlanItem.getFat(), mealPlanItem))")
    @Mapping(target = "fiber", expression = "java(scale(mealPlanItem.getFiber(), mealPlanItem))")
    @Mapping(target = "sugar", expression = "java(scale(mealPlanItem.getSugar(), mealPlanItem))")
    @Mapping(target = "calcium", expression = "java(scale(mealPlanItem.getCalcium(), mealPlanItem))")
    @Mapping(target = "sodium", expression = "java(scale(mealPlanItem.getSodium(), mealPlanItem))")
    MealPlanItemResponse toScaledResponse(MealPlanItem mealPlanItem);

    MealPlanItemIngredientResponse toResponse(MealPlanItemIngredient ingredient);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "mealPlanItemId", source = "mealPlanItemId")
    @Mapping(target = "name", expression = "java(trim(ingredient.getName()))")
    MealPlanItemIngredient toEntity(MealPlanItemIngredientUpsertRequest ingredient, Long mealPlanItemId);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "mealPlanId", source = "mealPlanId")
    @Mapping(target = "planDate", source = "generatedItem.planDate")
    @Mapping(target = "mealSlot", source = "generatedItem.mealSlot")
    @Mapping(target = "itemOrder", source = "generatedItem.itemOrder")
    @Mapping(target = "itemSource", expression = "java(MealPlanItemSource.RECIPE)")
    @Mapping(target = "recipeId", source = "generatedItem.candidate.recipeId")
    @Mapping(target = "postId", source = "generatedItem.candidate.postId")
    @Mapping(target = "customMealName", ignore = true)
    @Mapping(target = "plannedServings", source = "generatedItem.servings")
    @Mapping(target = "actualServings", ignore = true)
    @Mapping(target = "status", expression = "java(MealPlanItemStatus.PLANNED)")
    @Mapping(target = "recipeTitle", source = "generatedItem.candidate.title")
    @Mapping(target = "imageUrl", source = "generatedItem.candidate.imageUrl")
    @Mapping(target = "calories", expression = "java(toBigDecimal(generatedItem.getCandidate().getNutrition().getTotalCalories()))")
    @Mapping(target = "protein", expression = "java(toBigDecimal(generatedItem.getCandidate().getNutrition().getTotalProtein()))")
    @Mapping(target = "carbs", expression = "java(toBigDecimal(generatedItem.getCandidate().getNutrition().getTotalCarb()))")
    @Mapping(target = "fat", expression = "java(toBigDecimal(generatedItem.getCandidate().getNutrition().getTotalFat()))")
    @Mapping(target = "fiber", expression = "java(toBigDecimal(generatedItem.getCandidate().getNutrition().getTotalFiber()))")
    @Mapping(target = "sugar", expression = "java(toBigDecimal(generatedItem.getCandidate().getNutrition().getTotalSugar()))")
    @Mapping(target = "calcium", expression = "java(toBigDecimal(generatedItem.getCandidate().getNutrition().getTotalCalcium()))")
    @Mapping(target = "sodium", expression = "java(toBigDecimal(generatedItem.getCandidate().getNutrition().getTotalSodium()))")
    @Mapping(target = "note", source = "generatedItem.note")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    MealPlanItem toEntity(GeneratedMealItem generatedItem, Long mealPlanId);

    default String trim(String value) {
        return value == null ? null : value.trim();
    }

    default BigDecimal toBigDecimal(Double value) {
        return value == null ? null : BigDecimal.valueOf(value);
    }

    default BigDecimal scale(BigDecimal value, MealPlanItem item) {
        return value == null ? BigDecimal.ZERO : value.multiply(BigDecimal.valueOf(resolveServings(item)));
    }

    default int resolveServings(MealPlanItem item) {
        Integer servings = item.getActualServings() != null ? item.getActualServings() : item.getPlannedServings();
        return servings == null || servings <= 0 ? 1 : servings;
    }
}
