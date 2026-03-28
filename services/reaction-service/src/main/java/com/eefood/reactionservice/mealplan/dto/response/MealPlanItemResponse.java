package com.eefood.reactionservice.mealplan.dto.response;

import com.eefood.reactionservice.mealplan.enums.MealPlanItemSource;
import com.eefood.reactionservice.mealplan.enums.MealPlanItemStatus;
import com.eefood.reactionservice.mealplan.enums.MealSlot;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MealPlanItemResponse {
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
    private String recipeTitle;
    private String imageUrl;
    private BigDecimal calories;
    private BigDecimal protein;
    private BigDecimal carbs;
    private BigDecimal fat;
    private BigDecimal fiber;
    private BigDecimal sugar;
    private BigDecimal calcium;
    private BigDecimal sodium;
    private String note;
    private List<MealPlanItemIngredientResponse> ingredients;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
