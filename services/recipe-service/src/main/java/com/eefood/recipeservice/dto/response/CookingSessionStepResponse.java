package com.eefood.recipeservice.dto.response;
import com.eefood.recipeservice.enums.CookingStepStatus;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CookingSessionStepResponse {
    private Long cookingSessionStepId;
    private Long recipeStepId;
    private Integer stepNumber;
    private String instruction;
    private List<String> imageUrls;
    private List<String> videoUrls;
    private Integer stepTime;
    private CookingStepStatus status;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
}
