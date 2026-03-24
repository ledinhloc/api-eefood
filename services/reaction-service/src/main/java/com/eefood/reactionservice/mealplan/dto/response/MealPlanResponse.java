package com.eefood.reactionservice.mealplan.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MealPlanResponse {
    private Long id;
    private Long userId;
    private String goal;
    private LocalDate startDate;
    private LocalDate endDate;
    private String note;
    private String userHealthNote;
    private List<MealPlanItemResponse> items;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
