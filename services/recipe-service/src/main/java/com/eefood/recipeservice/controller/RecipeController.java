package com.eefood.recipeservice.controller;

import com.eefood.recipeservice.dto.request.RecipeRequest;
import com.eefood.recipeservice.dto.response.RecipeResponse;
import com.eefood.recipeservice.service.RecipeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/recipes")
@RequiredArgsConstructor
public class RecipeController {

  private final RecipeService recipeService;

  @GetMapping
  public List<RecipeResponse> getAllRecipes() {
    return recipeService.getAllRecipes();
  }

  @GetMapping("/{id}")
  public RecipeResponse getRecipe(@PathVariable Long id) {
    return recipeService.getRecipeById(id);
  }

  @PostMapping
  public RecipeResponse createRecipe(@RequestBody RecipeRequest request) {
    String currentUser = "loc";
    return recipeService.createRecipe(request, currentUser);
  }

  @PutMapping("/{id}")
  public RecipeResponse updateRecipe(@PathVariable Long id,
                                     @RequestBody RecipeRequest request) {
    String currentUser = "loc";
    return recipeService.updateRecipe(id, request, currentUser);
  }

  @GetMapping("/list/{authorId}")
  public List<RecipeResponse> getRecipeList(@PathVariable Long authorId) {
    return recipeService.getRecipesByUserId(authorId);
  }
}