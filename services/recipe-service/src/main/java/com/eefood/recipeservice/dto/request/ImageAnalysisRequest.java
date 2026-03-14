package com.eefood.recipeservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ImageAnalysisRequest {
    @NotBlank
    private String imageUrl;
}
