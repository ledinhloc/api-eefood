package com.eefood.reactionservice.mealplan.dto.ai;

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
public class GeneratedMealPlanResult {
    private String mealPlanNote;
    private List<GeneratedMealItem> items;
}
