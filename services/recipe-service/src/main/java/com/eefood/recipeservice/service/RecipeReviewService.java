package com.eefood.recipeservice.service;

import com.eefood.recipeservice.dto.request.RecipeReviewRequest;
import com.eefood.recipeservice.dto.response.ReviewQuestionResponse;
import com.eefood.recipeservice.enums.ErrorMessage;
import com.eefood.recipeservice.exception.ExceptionUtil;
import com.eefood.recipeservice.mapper.ReviewRecipeMapper;
import com.eefood.recipeservice.model.*;
import com.eefood.recipeservice.repository.RecipeRepository;
import com.eefood.recipeservice.repository.RecipeReviewRepository;
import com.eefood.recipeservice.repository.ReviewOptionRepository;
import com.eefood.recipeservice.repository.ReviewQuestionRepository;
import com.eefood.recipeservice.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RecipeReviewService {
    private final RecipeReviewRepository recipeReviewRepository;
    private final ReviewOptionRepository reviewOptionRepository;
    private final ReviewQuestionRepository reviewQuestionRepository;
    private final RecipeRepository recipeRepository;
    private final ReviewRecipeMapper reviewRecipeMapper;
    private final SecurityUtil securityUtil;


    public List<ReviewQuestionResponse> getReviewQuestion() {
        List<ReviewQuestion> rq = reviewQuestionRepository.findAllByIsDeletedIsFalseAndIsActiveTrue();

        return rq.stream()
                .map(reviewRecipeMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public void saveUserReview(Long recipeId, List<RecipeReviewRequest> request) {
        if (request == null || request.isEmpty()) {
            throw ExceptionUtil.badRequest(ErrorMessage.INVALID_REVIEW_REQUEST);
        }
        Recipe recipe = recipeRepository.findByIdAndIsDeletedFalse(recipeId)
                .orElseThrow(() -> ExceptionUtil.notFound(ErrorMessage.RECIPE_NOT_FOUND));
        Long userId = securityUtil.getCurrentUserId();

        Map<Long, ReviewQuestion> questionMap = reviewQuestionRepository
                .findAllByIsDeletedIsFalseAndIsActiveTrue()
                .stream().collect(Collectors.toMap(ReviewQuestion::getId, q -> q));

        Map<Long, ReviewOption> optionMap = reviewOptionRepository
                .findAllByIdInAndIsDeletedFalse(
                        request.stream().map(RecipeReviewRequest::getOptionId).collect(Collectors.toSet())
                ).stream().collect(Collectors.toMap(ReviewOption::getId, o -> o));

        Set<Long> questionIds = new HashSet<>();

        request.forEach(rr -> {
            if (!questionIds.add(rr.getQuestionId())) {
                throw ExceptionUtil.badRequest(ErrorMessage.DUPLICATE_QUESTION);
            }

            if (!questionMap.containsKey(rr.getQuestionId()))
                throw ExceptionUtil.badRequest(ErrorMessage.QUESTION_NOT_FOUND);

            if (!optionMap.containsKey(rr.getOptionId()))
                throw ExceptionUtil.badRequest(ErrorMessage.OPTION_NOT_FOUND);

            ReviewOption option = optionMap.get(rr.getOptionId());

            if (!option.getQuestion().getId().equals(rr.getQuestionId())) {
                throw ExceptionUtil.badRequest(ErrorMessage.INVALID_OPTION_FOR_QUESTION);
            }
        });

        int totalWeight = request.stream()
                .mapToInt(rr -> questionMap.get(rr.getQuestionId()).getWeight()).sum();
        double rating = totalWeight > 0
                ? request.stream().mapToInt(rr ->
                optionMap.get(rr.getOptionId()).getStarValue() *
                        questionMap.get(rr.getQuestionId()).getWeight()).sum() / (double) totalWeight
                : 0.0;

        RecipeReview review = recipeReviewRepository
                .findByUserIdAndRecipeIdAndIsDeletedIsFalse(userId, recipeId)
                .orElseGet(() -> RecipeReview.builder()
                        .userId(userId)
                        .recipe(recipe)
                        .totalWeight(0)
                        .rating(0.0)
                        .build());

        review.setRating(rating);
        review.setTotalWeight(totalWeight);
        review.getAnswers().clear();
        review.getAnswers().addAll(
                request.stream().map(rr -> ReviewAnswer.builder()
                        .review(review)
                        .question(questionMap.get(rr.getQuestionId()))
                        .option(optionMap.get(rr.getOptionId()))
                        .starValue(optionMap.get(rr.getOptionId()).getStarValue())
                        .weight(questionMap.get(rr.getQuestionId()).getWeight())
                        .build()
                ).collect(Collectors.toSet())
        );

        recipeReviewRepository.save(review);
    }
}
