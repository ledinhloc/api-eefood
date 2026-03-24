package com.eefood.reactionservice.mapper;

import com.eefood.reactionservice.dto.response.MealPlanItemIngredientResponse;
import com.eefood.reactionservice.dto.response.MealPlanItemResponse;
import com.eefood.reactionservice.dto.response.MealPlanResponse;
import com.eefood.reactionservice.mealplan.model.MealPlan;
import com.eefood.reactionservice.mealplan.model.MealPlanItem;
import com.eefood.reactionservice.mealplan.model.MealPlanItemIngredient;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface MealPlanMapper {
    MealPlanResponse toResponse(MealPlan mealPlan);

    MealPlanItemResponse toResponse(MealPlanItem mealPlanItem);

    MealPlanItemIngredientResponse toResponse(MealPlanItemIngredient ingredient);
}
