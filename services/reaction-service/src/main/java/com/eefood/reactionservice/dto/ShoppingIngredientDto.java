package com.eefood.reactionservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShoppingIngredientDto {
  private Long id;
  private Long ingredientId;
  private String ingredientName;
  private String image;
  private Double quantity;
  private String unit;
  private Boolean purchased;

  private List<Long> shoppingIngredientIds;
}
