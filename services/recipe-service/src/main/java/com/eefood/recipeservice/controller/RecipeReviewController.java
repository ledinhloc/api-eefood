package com.eefood.recipeservice.controller;

import com.eefood.recipeservice.dto.request.RecipeReviewRequest;
import com.eefood.recipeservice.dto.response.IngredientAlterResponse;
import com.eefood.recipeservice.dto.response.ResponseData;
import com.eefood.recipeservice.dto.response.ReviewQuestionResponse;
import com.eefood.recipeservice.service.RecipeReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/recipe-review")
@RequiredArgsConstructor
public class RecipeReviewController {
    private final RecipeReviewService recipeReviewService;
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
