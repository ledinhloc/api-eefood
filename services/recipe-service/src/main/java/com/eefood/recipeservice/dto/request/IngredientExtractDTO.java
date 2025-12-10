package com.eefood.recipeservice.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IngredientExtractDTO {
  private String name;
  private Double quantity;
  private String unit;
}
