package com.eefood.recipeservice.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecipeIngredientRequest {
    private Long id;
    private Long ingredientId;
    private String name;
    private Integer quantity;
    private String unit;
}
