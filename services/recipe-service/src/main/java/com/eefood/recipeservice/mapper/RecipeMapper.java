package com.eefood.recipeservice.mapper;

import com.eefood.recipeservice.dto.request.RecipeIngredientRequest;
import com.eefood.recipeservice.dto.request.RecipeRequest;
import com.eefood.recipeservice.dto.request.RecipeStepRequest;
import com.eefood.recipeservice.dto.response.*;
import com.eefood.recipeservice.dto.response.StepResponse;
import com.eefood.recipeservice.model.*;
import org.mapstruct.*;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface RecipeMapper {
  @Mappings({
    @Mapping(target = "difficulty", expression = "java(String.valueOf(recipe.getDifficulty()))")
  })
  RecipeDocument toDocument(Recipe recipe);

//  @Mapping(target = "ingredients", source = "ingredients", qualifiedByName = "filterDeletedIngredients")
//  @Mapping(target = "steps", source = "steps", qualifiedByName = "filterDeletedSteps")
  RecipeResponse toResponse(Recipe recipe);
  @Mapping(target = "recipeCategories", expression = "java(mapCategories(recipe.getCategories()))")
  @Mapping(target = "recipeIngredientKeywords", expression = "java(mapIngredientKeywords(recipe.getIngredients()))")
  RecipeSummaryResponse toSummaryResponse(Recipe recipe);
  RecipeDetailResponse toDetailResponse(Recipe recipe);

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

  @Mapping(target = "originalId", source = "id")
  IngredientDetailResponse toDetailResponse(Ingredient ingredient);

  Ingredient toEntity(IngredientDetailResponse response);

  //category
  CategoryResponse toResponse(Category category);

  default Set<String> mapCategories(Set<Category> categories) {
    if (categories == null) return new HashSet<>();
    return categories.stream()
      .filter(c -> !Boolean.TRUE.equals(c.getIsDeleted()))
      .map(Category::getDescription)
      .collect(Collectors.toSet());
  }

  default Set<String> mapIngredientKeywords(Set<RecipeIngredient> ingredients) {
    if (ingredients == null) return new HashSet<>();
    return ingredients.stream()
      .filter(i -> !Boolean.TRUE.equals(i.getIsDeleted()))
      .map(i -> i.getIngredient().getName())
      .collect(Collectors.toSet());
  }
//  @Named("filterDeletedIngredients")
//  default List<RecipeIngredientResponse> mapFilteredIngredients(Set<RecipeIngredient> ingredients) {
//    if (ingredients == null) return null;
//    return ingredients.stream()
//            .filter(ri -> !Boolean.TRUE.equals(ri.getIsDeleted()))
//            .map(this::toResponse)
//            .toList();
//  }

//  @Named("filterDeletedSteps")
//  default List<StepResponse> mapFilteredSteps(Set<RecipeStep> steps) {
//    if (steps == null) return null;
//    return steps.stream()
//            .filter(step -> !Boolean.TRUE.equals(step.getIsDeleted()))
//            .map(this::toResponse)
//            .toList();
//  }
}