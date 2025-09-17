package com.eefood.recipeservice.mapper;

import com.eefood.recipeservice.dto.request.RecipeRequest;
import com.eefood.recipeservice.dto.request.RecipeStepRequest;
import com.eefood.recipeservice.dto.response.*;
import com.eefood.recipeservice.dto.response.StepResponse;
import com.eefood.recipeservice.model.*;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface RecipeMapper {
  RecipeResponse toResponse(Recipe recipe);

  StepResponse toResponse(RecipeStep step);

// RecipeIngredient -> RecipeIngredientResponse
  RecipeIngredientResponse toResponse(RecipeIngredient recipeIngredient);

  @Mapping(target = "id", ignore = true)
  Recipe toEntity(RecipeRequest request);

  @Mapping(target = "id", ignore = true)
  RecipeStep toEntity(RecipeStepRequest request);

  // Ingredient -> IngredientResponse
  IngredientResponse toResponse(Ingredient ingredient);

  //category
  CategoryResponse toResponse(Category category);
}