package com.eefood.reactionservice.mealplan.mapper;

import com.eefood.reactionservice.mealplan.dto.response.MealPlanItemIngredientResponse;
import com.eefood.reactionservice.mealplan.dto.response.MealPlanItemResponse;
import com.eefood.reactionservice.mealplan.dto.response.MealPlanResponse;
import com.eefood.reactionservice.mealplan.model.MealPlan;
import com.eefood.reactionservice.mealplan.model.MealPlanItem;
import com.eefood.reactionservice.mealplan.model.MealPlanItemIngredient;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface MealPlanMapper {
    @Mapping(target = "items", ignore = true)
    MealPlanResponse toResponse(MealPlan mealPlan);

    @Mapping(target = "ingredients", ignore = true)
    MealPlanItemResponse toResponse(MealPlanItem mealPlanItem);

    MealPlanItemIngredientResponse toResponse(MealPlanItemIngredient ingredient);
}
