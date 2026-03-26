package com.eefood.reactionservice.mealplan.dto.ai;

import com.eefood.reactionservice.mealplan.enums.MealSlot;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GeneratedMealItem {
    private LocalDate planDate;
    private MealSlot mealSlot;
    private Integer itemOrder;
    private Integer servings;
    private String note;
    private MealPlanAiCandidate candidate;
}
