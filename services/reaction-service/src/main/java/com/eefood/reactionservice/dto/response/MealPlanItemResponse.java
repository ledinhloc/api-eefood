package com.eefood.reactionservice.dto.response;

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
    private String recipeTitleSnapshot;
    private String imageUrlSnapshot;
    private Integer recipeServingsSnapshot;
    private BigDecimal caloriesPerServingSnapshot;
    private BigDecimal proteinPerServingSnapshot;
    private BigDecimal carbsPerServingSnapshot;
    private BigDecimal fatPerServingSnapshot;
    private BigDecimal fiberPerServingSnapshot;
    private BigDecimal sugarPerServingSnapshot;
    private BigDecimal calciumPerServingSnapshot;
    private BigDecimal sodiumPerServingSnapshot;
    private String note;
    private List<MealPlanItemIngredientResponse> ingredients;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
