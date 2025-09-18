package com.eefood.recipeservice.dto.response;

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
public class RecipeResponse {
  private Long id;
  private String title;
  private String description;
  private String region;
  private String imageUrl;
  private String videoUrl;
  private Integer prepTime;
  private Integer cookTime;
  private Difficulty difficulty;

  private List<CategoryResponse> categories;
  private List<StepResponse> steps;
  private List<RecipeIngredientResponse> ingredients;
}