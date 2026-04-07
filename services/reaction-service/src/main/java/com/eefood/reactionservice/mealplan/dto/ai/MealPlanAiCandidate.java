package com.eefood.reactionservice.mealplan.dto.ai;

import com.eefood.reactionservice.mealplan.dto.response.NutritionAnalysisResponse;
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
public class MealPlanAiCandidate {
    private Long recipeId;
    private Long postId;
    private String title;
    private String description;
    private String imageUrl;
    private String region;
    private Integer prepTime;
    private Integer cookTime;
    private String difficulty;
    private List<String> ingredientKeywords;
    private NutritionAnalysisResponse nutrition;
}
