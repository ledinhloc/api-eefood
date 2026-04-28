package com.eefood.recipeservice.dto.request;

import com.eefood.recipeservice.dto.response.ReviewOptionResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecipeReviewRequest {
    private Long questionId;
    private Long optionId;
    private Integer starValue;
}
