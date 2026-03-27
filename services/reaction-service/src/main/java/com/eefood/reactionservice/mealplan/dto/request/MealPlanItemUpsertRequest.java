package com.eefood.reactionservice.mealplan.dto.request;

import com.eefood.reactionservice.mealplan.enums.MealPlanItemSource;
import com.eefood.reactionservice.mealplan.enums.MealPlanItemStatus;
import com.eefood.reactionservice.mealplan.enums.MealSlot;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MealPlanItemUpsertRequest {
    private Long id;
    private LocalDate planDate;
    private MealSlot mealSlot;
    private Integer itemOrder;
    private MealPlanItemSource itemSource;
    private Long recipeId;
    private Long postId;
    private String customMealName;
    private Integer plannedServings;
    private Integer actualServings;
    private MealPlanItemStatus status;
    private String note;
    private List<MealPlanItemIngredientUpsertRequest> ingredients;
}
