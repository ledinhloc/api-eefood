package com.eefood.reactionservice.mealplan.mapper;

import com.eefood.reactionservice.mealplan.dto.request.MealPlanUpsertRequest;
import com.eefood.reactionservice.mealplan.dto.response.MealPlanResponse;
import com.eefood.reactionservice.mealplan.model.MealPlan;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(componentModel = "spring")
public interface MealPlanMapper {
    @Mapping(target = "items", ignore = true)
    MealPlanResponse toResponse(MealPlan mealPlan);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateFromRequest(MealPlanUpsertRequest request, @MappingTarget MealPlan mealPlan);

    @AfterMapping
    default void setDefaultItems(@MappingTarget MealPlanResponse response) {
        response.setItems(List.of());
    }
}
