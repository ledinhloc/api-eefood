package com.eefood.reactionservice.mealplan.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NutritionAnalysisResponse {
    private Long recipeId;
    private String recipeTitle;
    private Double totalCalories;
    private Double totalProtein;
    private Double totalFat;    
    private Double totalCarb;
    private Double totalFiber;
    private Double totalSugar;
    private Double totalCalcium;
    private Double totalSodium;
    private Double healthScore;
    private String summary;
    private String healthLevel;
    private String recommendation;
    private List<IngredientNutritionDetailResponse> ingredientDetails;
}
