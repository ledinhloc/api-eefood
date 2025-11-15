package com.eefood.recipeservice.dto.response;

import com.eefood.recipeservice.dto.request.RecipeStepRequest;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecipeExtractDTO {
  private String title;
  private String description;
  private String region;
  private String imageUrl;
  private String videoUrl;
  private List<String> categories;
  private Integer prepTime;
  private Integer cookTime;
  private String difficulty;
  private List<IngredientExtractDTO> ingredients;
  private List<RecipeStepRequest> steps;
}
