package com.eefood.recipeservice.mapper;

import com.eefood.recipeservice.dto.response.CookingSessionProgressResponse;
import com.eefood.recipeservice.dto.response.CookingSessionResponse;
import com.eefood.recipeservice.dto.response.CookingSessionStepResponse;
import com.eefood.recipeservice.model.CookingSessionStep;
import com.eefood.recipeservice.model.CookingSessions;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface CookingSessionMapper {

    @Mapping(target = "cookingSessionStepId", source = "id")
    @Mapping(target = "recipeStepId",         source = "recipeStep.id")
    @Mapping(target = "stepNumber",           source = "recipeStep.stepNumber")
    @Mapping(target = "instruction",          source = "recipeStep.instruction")
    @Mapping(target = "imageUrls",            source = "recipeStep.imageUrls")
    @Mapping(target = "videoUrls",            source = "recipeStep.videoUrls")
    @Mapping(target = "stepTime",             source = "recipeStep.stepTime")
    CookingSessionStepResponse toStepResponse(CookingSessionStep step);

    List<CookingSessionStepResponse> toStepResponseList(List<CookingSessionStep> steps);

    @Mapping(target = "sessionId",   source = "id")
    @Mapping(target = "recipeId",    source = "recipe.id")
    @Mapping(target = "recipeTitle", source = "recipe.title")
    @Mapping(target = "totalSteps",  ignore = true)
    @Mapping(target = "steps",       ignore = true)
    CookingSessionResponse toResponse(CookingSessions session);

    @Mapping(target = "sessionId",  source = "id")
    @Mapping(target = "totalSteps", ignore = true)
    CookingSessionProgressResponse toProgressResponse(CookingSessions session);
}
