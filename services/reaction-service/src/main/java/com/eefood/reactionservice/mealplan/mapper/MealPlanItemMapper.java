package com.eefood.reactionservice.mealplan.mapper;

import com.eefood.reactionservice.mealplan.dto.request.MealPlanItemIngredientUpsertRequest;
import com.eefood.reactionservice.mealplan.dto.response.MealPlanItemIngredientResponse;
import com.eefood.reactionservice.mealplan.dto.response.MealPlanItemResponse;
import com.eefood.reactionservice.mealplan.model.MealPlanItem;
import com.eefood.reactionservice.mealplan.model.MealPlanItemIngredient;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface MealPlanItemMapper {
    @Mapping(target = "ingredients", ignore = true)
    MealPlanItemResponse toResponse(MealPlanItem mealPlanItem);

    MealPlanItemIngredientResponse toResponse(MealPlanItemIngredient ingredient);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "mealPlanItemId", source = "mealPlanItemId")
    @Mapping(target = "name", expression = "java(trim(ingredient.getName()))")
    MealPlanItemIngredient toEntity(MealPlanItemIngredientUpsertRequest ingredient, Long mealPlanItemId);

    default String trim(String value) {
        return value == null ? null : value.trim();
    }
}
