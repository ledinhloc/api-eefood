package com.eefood.recipeservice.dto.response;

import com.eefood.recipeservice.enums.CookingSessionStatus;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CookingSessionResponse {
    private Long sessionId;
    private Long recipeId;
    private String recipeTitle;
    private CookingSessionStatus status;
    private Integer currentStep;
    private Integer totalSteps;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private List<CookingSessionStepResponse> steps;
}
