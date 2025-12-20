package com.eefood.recipeservice.dto.request;

import com.eefood.recipeservice.enums.Difficulty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecipeRequest {
  private String title;
  private String description;
  private String region;
  private String imageUrl;
  private String videoUrl;
  private Integer prepTime;
  private Integer cookTime;
  private Difficulty difficulty;

//  private List<Long> categoryIds;//dung khi update
  private List<String> categories;//khi create
  private List<RecipeIngredientRequest> ingredients;
  private List<RecipeStepRequest> steps;
}