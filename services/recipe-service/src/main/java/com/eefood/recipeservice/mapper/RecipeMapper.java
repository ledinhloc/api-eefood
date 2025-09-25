package com.eefood.recipeservice.mapper;

import com.eefood.recipeservice.dto.request.RecipeIngredientRequest;
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
  @Mapping(target = "ingredients", ignore = true)
  @Mapping(target = "steps", ignore = true)
  @Mapping(target = "categories", ignore = true)
  Recipe toEntity(RecipeRequest request);

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "recipe", ignore = true)
  RecipeStep toEntity(RecipeStepRequest request);

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "recipe", ignore = true)
  @Mapping(target = "ingredient", ignore = true)
  RecipeIngredient toEntity(RecipeIngredientRequest request);

  // Ingredient -> IngredientResponse
  IngredientResponse toResponse(Ingredient ingredient);

  //category
  CategoryResponse toResponse(Category category);
}