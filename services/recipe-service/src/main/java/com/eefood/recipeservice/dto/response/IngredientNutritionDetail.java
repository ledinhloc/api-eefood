package com.eefood.recipeservice.dto.response;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class IngredientNutritionDetail {
    private String ingredientName;
    private Double quantity;
    private String unit;
    private Double calories;
    private Double protein;
    private Double fat;
    private Double carb;
}
