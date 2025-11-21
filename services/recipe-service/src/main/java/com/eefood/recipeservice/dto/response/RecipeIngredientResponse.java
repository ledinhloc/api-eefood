package com.eefood.recipeservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecipeIngredientResponse {
  private Long id;           // id của RecipeIngredient
  private Double quantity;
  private String unit;

  private IngredientResponse ingredient; // chứa thông tin nguyên liệu
}
