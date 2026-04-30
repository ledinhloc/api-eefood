package com.eefood.recipeservice.controller;

import com.eefood.recipeservice.dto.request.RecipeReviewRequest;
import com.eefood.recipeservice.dto.response.RecipeReviewStatsResponse;
import com.eefood.recipeservice.dto.response.ResponseData;
import com.eefood.recipeservice.dto.response.ReviewDetailResponse;
import com.eefood.recipeservice.dto.response.ReviewQuestionResponse;
import com.eefood.recipeservice.service.RecipeReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/recipe-review")
@RequiredArgsConstructor
public class RecipeReviewController {
    private final RecipeReviewService recipeReviewService;

    @GetMapping("/{recipeId}/stats")
    public ResponseData<RecipeReviewStatsResponse> getRecipeReviewStats(@PathVariable Long recipeId)
    {
        RecipeReviewStatsResponse response = recipeReviewService.getRecipeReviewStats(recipeId);
        return new ResponseData<>(200, "Get success", response);
    }

    @GetMapping("/{recipeId}/list")
    public ResponseData<Page<ReviewDetailResponse>> getRecipeReviews(
            @PathVariable Long recipeId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size)
    {
        Pageable pageable = PageRequest.of(page-1, size, Sort.by("createdAt").descending());
        return new ResponseData<>(200, "Get success", recipeReviewService.getRecipeReviews(recipeId, pageable));
    }

    @GetMapping
    public ResponseData<List<ReviewQuestionResponse>> getListReviewQuestion() {
        List<ReviewQuestionResponse> responses = recipeReviewService.getReviewQuestion();
        return new ResponseData<>(200, "Get success", responses);
    }

    @PostMapping("/{recipeId}")
    public ResponseData<Void> saveUserReview(@PathVariable Long recipeId, @RequestBody List<RecipeReviewRequest> request) {
        recipeReviewService.saveUserReview(recipeId, request);
        return new ResponseData<>(200, "Success");
    }
}
