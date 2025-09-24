package com.eefood.recipeservice.service;

import com.eefood.recipeservice.dto.request.RecipeIngredientRequest;
import com.eefood.recipeservice.dto.request.RecipeRequest;
import com.eefood.recipeservice.dto.request.RecipeStepRequest;
import com.eefood.recipeservice.dto.response.RecipeResponse;
import com.eefood.recipeservice.enums.Difficulty;
import com.eefood.recipeservice.mapper.RecipeMapper;
import com.eefood.recipeservice.model.*;
import com.eefood.recipeservice.repository.CategoryRepository;
import com.eefood.recipeservice.repository.IngredientRepository;
import com.eefood.recipeservice.repository.RecipeRepository;
import com.eefood.recipeservice.repository.RecipeStepRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RecipeService {
  private final RecipeRepository recipeRepository;
  private final RecipeStepRepository stepRepository;
  private final CategoryRepository categoryRepository;
  private final RecipeMapper recipeMapper;
  private final IngredientRepository ingredientRepository;

  @Transactional(readOnly = true)
  public Page<RecipeResponse> searchRecipes(
    String title,
    String description,
    String region,
    Difficulty difficulty,
    Long categoryId,
    Long authorId,
    Pageable pageable
  ) {
    Specification<Recipe> spec = Specification.allOf(
      RecipeSpecification.isNotDeleted(),
      RecipeSpecification.hasTitle(title),
      RecipeSpecification.hasDescription(description),
      RecipeSpecification.hasRegion(region),
      RecipeSpecification.hasDifficulty(difficulty),
      RecipeSpecification.hasCategoryId(categoryId),
      RecipeSpecification.withFetchJoin(),
      RecipeSpecification.hasAuthor(authorId)
    );
    return recipeRepository.findAll(spec, pageable).map(recipeMapper::toResponse);
  }

  @Transactional(readOnly = true)
  public RecipeResponse getRecipeById(Long id) {
    Recipe recipe = recipeRepository.findById(id)
      .orElseThrow(() -> new EntityNotFoundException("Recipe not found"));
    return recipeMapper.toResponse(recipe);
  }

  @Transactional
  public RecipeResponse createRecipe(RecipeRequest request, String currentUser) {
    Recipe recipe = recipeMapper.toEntity(request);

    // set categories
    List<Category> categories = categoryRepository.findAllById(request.getCategoryIds());
    recipe.setCategories(new HashSet<>(categories));

    // set ingredients
    if(request.getIngredients() != null) {
      for (RecipeIngredientRequest ingredientReq : request.getIngredients()){
        RecipeIngredient recipeIngredient = recipeMapper.toEntity(ingredientReq);
        Ingredient ingredient = ingredientRepository.findById(ingredientReq.getIngredientId())
                .orElseThrow(() -> new EntityNotFoundException("Ingredient not found with id: " + ingredientReq.getIngredientId()));
        recipeIngredient.setIngredient(ingredient);
        recipe.addIngredient(recipeIngredient);
      }
    }

    // add steps
    if (request.getSteps() != null) {
      for (RecipeStepRequest stepReq : request.getSteps()) {
        RecipeStep step = recipeMapper.toEntity(stepReq);
        recipe.addStep(step);
      }
    }
    Recipe saved = recipeRepository.save(recipe);
    return recipeMapper.toResponse(saved);
  }

  @Transactional
  public RecipeResponse updateRecipe(Long id, RecipeRequest request, String currentUser) {
    Recipe recipe = recipeRepository.findById(id)
      .orElseThrow(() -> new EntityNotFoundException("Recipe not found"));

    recipe.setTitle(request.getTitle());
    recipe.setDescription(request.getDescription());
    recipe.setRegion(request.getRegion());
    recipe.setImageUrl(request.getImageUrl());
    recipe.setVideoUrl(request.getVideoUrl());
    recipe.setPrepTime(request.getPrepTime());
    recipe.setCookTime(request.getCookTime());
    recipe.setDifficulty(request.getDifficulty());
    recipe.setUpdatedBy(currentUser);

    // update categories
    List<Category> categories = categoryRepository.findAllById(request.getCategoryIds());
//    recipe.setCategories(categories);

    // update steps
    List<Long> requestStepIds = request.getSteps().stream()
      .map(RecipeStepRequest::getId)
      .filter(Objects::nonNull)
      .toList();

    List<RecipeStep> existingSteps = stepRepository.findByRecipeIdAndIsDeletedFalse(id);

    // soft delete missing steps
    for (RecipeStep step : existingSteps) {
      if (!requestStepIds.contains(step.getId())) {
        step.setIsDeleted(true);
        step.setUpdatedBy(currentUser);
        stepRepository.save(step);
      }
    }

    // update or create steps
    for (RecipeStepRequest stepReq : request.getSteps()) {
      if (stepReq.getId() == null) {
        RecipeStep newStep = recipeMapper.toEntity(stepReq);
        newStep.setRecipe(recipe);
        newStep.setCreatedBy(currentUser);
        newStep.setUpdatedBy(currentUser);
        stepRepository.save(newStep);
      } else {
        RecipeStep step = stepRepository.findById(stepReq.getId())
          .orElseThrow(() -> new EntityNotFoundException("Step not found"));
        step.setStepNumber(stepReq.getStepNumber());
        step.setInstruction(stepReq.getInstruction());
        step.setImageUrl(stepReq.getImageUrl());
        step.setVideoUrl(stepReq.getVideoUrl());
        step.setStepTime(stepReq.getStepTime());
        step.setUpdatedBy(currentUser);
        stepRepository.save(step);
      }
    }

    return recipeMapper.toResponse(recipe);
  }
}