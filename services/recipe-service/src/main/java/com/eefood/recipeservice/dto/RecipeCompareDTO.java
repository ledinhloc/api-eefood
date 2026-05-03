package com.eefood.recipeservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecipeCompareDTO {
    private Long id;
    private String title;
    private String imageUrl;
    private String videoUrl;
    private Integer prepTime;
    private Integer cookTime;
    private Integer totalTime;
    private String difficulty;
    private String region;
    private int stepCount;
    private int ingredientCount;

    private Double calories;
    private Double protein;
    private Double fat;
    private Double carb;
    private Double fiber;
    private Double sugar;
    private Double cal;
    private Double sodium;
    private Double healthScore;
}
