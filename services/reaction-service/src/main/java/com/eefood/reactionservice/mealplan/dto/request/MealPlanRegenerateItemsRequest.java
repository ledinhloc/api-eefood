package com.eefood.reactionservice.mealplan.dto.request;

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
public class MealPlanRegenerateItemsRequest {
    private List<Long> itemIds;
    private String reason;
}
