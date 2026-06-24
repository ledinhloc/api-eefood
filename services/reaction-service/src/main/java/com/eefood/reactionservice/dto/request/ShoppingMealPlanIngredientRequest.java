package com.eefood.reactionservice.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShoppingMealPlanIngredientRequest {
  private String name;
  private Double quantity;
  private String unit;
}
