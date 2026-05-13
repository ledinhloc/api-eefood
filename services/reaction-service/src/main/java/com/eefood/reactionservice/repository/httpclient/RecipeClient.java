package com.eefood.reactionservice.repository.httpclient;

import com.eefood.reactionservice.dto.response.CategoryResponse;
import com.eefood.reactionservice.dto.response.RecipeSummaryResponse;
import com.eefood.reactionservice.dto.response.ResponseData;
import com.eefood.reactionservice.dto.response.ShoppingItemDto;
import com.eefood.reactionservice.mealplan.dto.response.NutritionAnalysisResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "recipe-service")
public interface RecipeClient {
  @GetMapping("/api/v1/recipes/search-ids")
  ResponseData<List<Long>> searchRecipeIds(
    @RequestParam(required = false) String keyword,
    @RequestParam(required = false) String region,
    @RequestParam(required = false) String difficulty
  );

  @GetMapping("/api/v1/recipes/summary/{id}")
  ResponseData<RecipeSummaryResponse> getRecipeSummary(@PathVariable("id") Long id);

  @GetMapping("/api/v1/categories/all")
  ResponseData<List<CategoryResponse>> getListOfCategories();

  @GetMapping("/api/v1/nutrition/recipe/{recipeId}")
  ResponseData<NutritionAnalysisResponse> getNutritionByRecipeId(
          @PathVariable("recipeId") Long recipeId,
          @RequestParam(defaultValue = "false") boolean forceRefresh
  );

  @PostMapping("/api/v1/nutrition/recipe/{recipeId}/stream")
  String analyzeRecipeNutritionStream(
          @PathVariable("recipeId") Long recipeId,
          @RequestParam(defaultValue = "false") boolean forceRefresh
  );

  @PostMapping("/api/v1/shopping/chatbot/add")
  ResponseData<ShoppingItemDto> addRecipe(
          @RequestParam Long recipeId,
          @RequestParam Long userId,
          @RequestParam(defaultValue = "1") Integer servings
  );

  @GetMapping("/api/v1/nutrition/recipe/{recipeId}/chatbot")
  ResponseData<NutritionAnalysisResponse> getNutritionByRecipeIdForChatbot(
          @PathVariable("recipeId") Long recipeId,
          @RequestParam(defaultValue = "false") boolean forceRefresh
  );
}
