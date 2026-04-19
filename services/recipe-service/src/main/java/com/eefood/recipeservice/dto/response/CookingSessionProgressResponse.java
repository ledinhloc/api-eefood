package com.eefood.recipeservice.dto.response;

import com.eefood.recipeservice.enums.CookingSessionStatus;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CookingSessionProgressResponse {
    private Long sessionId;
    private Integer currentStep;
    private Integer totalSteps;
    private CookingSessionStatus status;
}
