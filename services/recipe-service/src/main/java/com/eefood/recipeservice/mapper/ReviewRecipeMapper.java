package com.eefood.recipeservice.mapper;

import com.eefood.recipeservice.dto.response.ReviewQuestionResponse;
import com.eefood.recipeservice.model.ReviewQuestion;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ReviewRecipeMapper {
    ReviewQuestionResponse toResponse(ReviewQuestion reviewQuestion);
}
