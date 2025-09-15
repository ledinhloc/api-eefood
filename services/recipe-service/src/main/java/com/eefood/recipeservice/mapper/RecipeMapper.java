package com.eefood.recipeservice.mapper;

import com.eefood.recipeservice.dto.request.RecipeRequest;
import com.eefood.recipeservice.dto.request.RecipeStepRequest;
import com.eefood.recipeservice.dto.response.CategoryResponse;
import com.eefood.recipeservice.dto.response.RecipeResponse;
import com.eefood.recipeservice.dto.response.RecipeStepResponse;
import com.eefood.recipeservice.model.Category;
import com.eefood.recipeservice.model.Recipe;
import com.eefood.recipeservice.model.RecipeStep;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface RecipeMapper {

  @Mapping(target = "categories", source = "categories")
  @Mapping(target = "steps", source = "steps")
  RecipeResponse toResponse(Recipe recipe);

  CategoryResponse toCategoryResponse(Category category);

  RecipeStepResponse toStepResponse(RecipeStep step);

  @Mapping(target = "id", ignore = true)
  Recipe toEntity(RecipeRequest request);

  @Mapping(target = "id", ignore = true)
  RecipeStep toEntity(RecipeStepRequest request);
}
