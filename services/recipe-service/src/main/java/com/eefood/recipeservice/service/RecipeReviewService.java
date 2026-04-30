package com.eefood.recipeservice.service;

import com.eefood.recipeservice.dto.request.RecipeReviewRequest;
import com.eefood.recipeservice.dto.response.*;
import com.eefood.recipeservice.enums.ErrorMessage;
import com.eefood.recipeservice.exception.ExceptionUtil;
import com.eefood.recipeservice.mapper.ReviewRecipeMapper;
import com.eefood.recipeservice.model.*;
import com.eefood.recipeservice.repository.*;
import com.eefood.recipeservice.repository.httpclient.IamClient;
import com.eefood.recipeservice.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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
    private final ReviewAnswerRepository reviewAnswerRepository;
    private final RecipeRepository recipeRepository;
    private final ReviewRecipeMapper reviewRecipeMapper;
    private final SecurityUtil securityUtil;
    private final IamClient iamClient;

    public Page<ReviewDetailResponse> getRecipeReviews(Long recipeId, Pageable pageable) {
        recipeRepository.findByIdAndIsDeletedFalse(recipeId)
                .orElseThrow(() -> ExceptionUtil.notFound(ErrorMessage.RECIPE_NOT_FOUND));

        Page<RecipeReview> reviewPage = recipeReviewRepository.findAllByRecipeId(recipeId, pageable);
        List<ReviewDetailResponse> content = enrichReviews(reviewPage.getContent());

        return new PageImpl<>(content, pageable, reviewPage.getTotalElements());
    }

    public RecipeReviewStatsResponse getRecipeReviewStats(Long recipeId) {
        recipeRepository.findByIdAndIsDeletedFalse(recipeId)
                .orElseThrow(() -> ExceptionUtil.notFound(ErrorMessage.RECIPE_NOT_FOUND));

        List<Object[]> rawStats = recipeReviewRepository.getCountAndAvgRating(recipeId);

        long totalReviews = 0L;
        double averageRating = 0.0;

        if (!rawStats.isEmpty()) {
            Object[] row = rawStats.get(0);
            totalReviews = row[0] != null ? ((Number) row[0]).longValue() : 0L;
            averageRating = row[1] != null
                    ? Math.round(((Number) row[1]).doubleValue() * 10.0) / 10.0
                    : 0.0;
        }

        Map<Integer, Long> ratingDistribution = buildRatingDistribution(
                recipeReviewRepository.getRatingDistribution(recipeId)
        );

        List<QuestionStatResponse> questionStats = buildQuestionStats(recipeId);

        List<RecipeReview> top5 = recipeReviewRepository.findTop5ByRecipeId(
                recipeId, PageRequest.of(0, 5)
        );
        List<ReviewDetailResponse> reviewDetails = enrichReviews(top5);

        return RecipeReviewStatsResponse.builder()
                .avgRating(averageRating)
                .ratingDistribution(ratingDistribution)
                .totalReviews(totalReviews)
                .questionStats(questionStats)
                .reviews(reviewDetails)
                .build();
    }

    private Map<Integer, Long> buildRatingDistribution(List<Object[]> rawData) {
        // Khởi tạo mặc định 1->5 = 0
        Map<Integer, Long> distribution = new LinkedHashMap<>();
        for (int i = 5; i >= 1; i--) distribution.put(i, 0L);

        rawData.forEach(row -> {
            int star = ((Number) row[0]).intValue();
            long count = (Long) row[1];
            // Map về mức gần nhất trong 1-5
            int key = Math.max(1, Math.min(5, star));
            distribution.merge(key, count, Long::sum);
        });

        return distribution;
    }

    private List<QuestionStatResponse> buildQuestionStats(Long recipeId) {
        List<ReviewQuestion> questions = reviewQuestionRepository
                .findAllByIsDeletedIsFalseAndIsActiveTrue();

        // Map: questionId -> optionId -> count
        Map<Long, Map<Long, Long>> answerCountMap = new HashMap<>();
        reviewAnswerRepository.countAnswersByRecipeId(recipeId).forEach(row -> {
            Long qId = (Long) row[0];
            Long oId = (Long) row[1];
            Long cnt = (Long) row[2];
            answerCountMap.computeIfAbsent(qId, k -> new HashMap<>()).put(oId, cnt);
        });

        return questions.stream().map(q -> {
            Map<Long, Long> optionCounts = answerCountMap.getOrDefault(q.getId(), Map.of());
            long totalAnswers = optionCounts.values().stream().mapToLong(Long::longValue).sum();

            List<OptionStatResponse> optionStats = q.getOptions().stream()
                    .map(opt -> {
                        long count = optionCounts.getOrDefault(opt.getId(), 0L);
                        double percent = totalAnswers > 0
                                ? Math.round(count * 100.0 / totalAnswers * 10) / 10.0
                                : 0.0;
                        return OptionStatResponse.builder()
                                .optionId(opt.getId())
                                .content(opt.getContent())
                                .count(count)
                                .percent(percent)
                                .build();
                    })
                    .sorted(Comparator.comparingLong(OptionStatResponse::getCount).reversed())
                    .collect(Collectors.toList());

            return QuestionStatResponse.builder()
                    .questionId(q.getId())
                    .content(q.getContent())
                    .weight(q.getWeight())
                    .options(optionStats)
                    .build();
        }).collect(Collectors.toList());
    }

    private List<ReviewDetailResponse> enrichReviews(List<RecipeReview> reviews) {
        if (reviews.isEmpty()) return List.of();

        List<Long> userIds = reviews.stream().map(RecipeReview::getUserId).distinct().collect(Collectors.toList());
        Map<Long, UserInfo> userInfoMap = fetchUserInfoMap(userIds);

        return reviews.stream().map(review -> {
            UserInfo user = userInfoMap.getOrDefault(review.getUserId(), null);

            List<ReviewAnswer> answers = new ArrayList<>(review.getAnswers());

            List<String> tags = answers.stream()
                    .map(a -> a.getOption().getContent())
                    .collect(Collectors.toList());

            return ReviewDetailResponse.builder()
                    .reviewId(review.getId())
                    .userId(review.getUserId())
                    .name(user != null ? user.getUsername() : "Unknown")
                    .avatar(user != null ? user.getAvatarUrl() : null)
                    .rating(review.getRating())
                    .createdAt(review.getCreatedAt())
                    .tags(tags)
                    .build();
        }).collect(Collectors.toList());
    }

    private Map<Long, UserInfo> fetchUserInfoMap(List<Long> userIds) {
        try {
            ResponseData<List<UserInfo>> response = iamClient.getUserInfoBatch(userIds);
            if (response != null && response.getData() != null) {
                return response.getData().stream()
                        .collect(Collectors.toMap(UserInfo::getId, u -> u));
            }
        } catch (Exception e) {
            log.warn("Failed to fetch user info batch: {}", e.getMessage());
        }
        return Map.of();
    }

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
