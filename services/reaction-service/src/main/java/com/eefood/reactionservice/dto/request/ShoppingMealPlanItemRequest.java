package com.eefood.reactionservice.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShoppingMealPlanItemRequest {
  private Long recipeId;
  private String recipeTitle;
  private Integer servings;
  private List<ShoppingMealPlanIngredientRequest> ingredients;
}
