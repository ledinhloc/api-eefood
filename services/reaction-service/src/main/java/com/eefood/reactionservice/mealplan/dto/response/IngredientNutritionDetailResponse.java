package com.eefood.reactionservice.mealplan.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IngredientNutritionDetailResponse {
    private String ingredientName;
    private Double quantity;
    private String unit;
    private Double calories;
    private Double protein;
    private Double fat;
    private Double carb;
    private Double fiber;
    private Double sugar;
    private Double calcium;
    private Double sodium;
}
