package com.eefood.reactionservice.mealplan.dto.request;

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
public class MealPlanItemIngredientUpsertRequest {
    private String name;
    private Double quantity;
    private String unit;
    private String note;
}
