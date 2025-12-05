package com.eefood.recipeservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShoppingItemDto {
  private Long id;
  private Long recipeId;
  private String recipeTitle;
  private Integer servings;
  private List<ShoppingIngredientDto> ingredients;
}
