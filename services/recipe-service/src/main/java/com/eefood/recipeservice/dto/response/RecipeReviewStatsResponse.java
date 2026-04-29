package com.eefood.recipeservice.dto.response;

import lombok.*;

import java.util.List;
import java.util.Map;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RecipeReviewStatsResponse {
    private double avgRating;
    private Long totalReviews;
    private Map<Integer, Long> ratingDistribution;
    private List<QuestionStatResponse> questionStats;
    private List<ReviewDetailResponse> reviews;
}
