package com.eefood.reactionservice.mealplan.dto.ai;

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
public class GeneratedMealReplacement {
    private Long mealPlanItemId;
    private Integer servings;
    private String note;
    private MealPlanAiCandidate candidate;
}
