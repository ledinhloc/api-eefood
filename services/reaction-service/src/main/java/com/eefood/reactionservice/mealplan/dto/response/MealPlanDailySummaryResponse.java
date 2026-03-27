package com.eefood.reactionservice.mealplan.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MealPlanDailySummaryResponse {
    private LocalDate planDate;
    private BigDecimal calories;//kcal
    private BigDecimal protein;
    private BigDecimal carbs;//tinh bột
    private BigDecimal fat;//chat beo
    private BigDecimal fiber;//chất xơ
    private BigDecimal sugar;//đường
    private BigDecimal sodium;//natri
    private BigDecimal calcium;//calcium
}
