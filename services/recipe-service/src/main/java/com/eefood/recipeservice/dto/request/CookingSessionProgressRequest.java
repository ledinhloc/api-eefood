package com.eefood.recipeservice.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CookingSessionProgressRequest {
    @NotNull(message = "currentStep is required")
    @Min(value = 1, message = "currentStep must be at least 1")
    private Integer currentStep;
}
