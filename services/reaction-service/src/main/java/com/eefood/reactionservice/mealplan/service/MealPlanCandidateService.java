package com.eefood.reactionservice.mealplan.service;

import com.eefood.reactionservice.dto.response.UserResponse;
import com.eefood.reactionservice.enums.PostStatus;
import com.eefood.reactionservice.mealplan.dto.ai.MealPlanAiCandidate;
import com.eefood.reactionservice.mealplan.dto.response.NutritionAnalysisResponse;
import com.eefood.reactionservice.mealplan.enums.MealSlot;
import com.eefood.reactionservice.mealplan.mapper.MealPlanAiMapper;
import com.eefood.reactionservice.mealplan.model.MealPlan;
import com.eefood.reactionservice.mealplan.model.MealPlanItem;
import com.eefood.reactionservice.mealplan.repo.MealPlanItemRepository;
import com.eefood.reactionservice.mealplan.repo.MealPlanRepository;
import com.eefood.reactionservice.model.Post;
import com.eefood.reactionservice.repository.httpclient.RecipeClient;
import com.eefood.reactionservice.repository.post.PostRepository;
import com.eefood.reactionservice.service.chatbot.ChromaRagService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MealPlanCandidateService {
    private static final int POST_LIMIT = 30;
    private static final int CANDIDATE_LIMIT = 15;
    private static final int SEMANTIC_LIMIT = 50;

    private final PostRepository postRepository;
    private final RecipeClient recipeClient;
    private final MealPlanRepository mealPlanRepository;
    private final MealPlanItemRepository mealPlanItemRepository;
    private final MealPlanAiMapper mealPlanAiMapper;
    private final MealPlanNutritionScoringService mealPlanNutritionScoringService;
    private final ChromaRagService chromaRagService;

    public List<MealPlanAiCandidate> loadInitialMealPlanCandidates(UserResponse user, String goal, Set<Long> recentRecipeIds) {
        String semanticQuery = goal == null || goal.isBlank()
                ? null
                : "Tìm món ăn phù hợp mục tiêu: " + goal.trim();
        return loadCandidateRecipes(
                user,
                goal,
                Set.of(),
                recentRecipeIds == null ? Set.of() : recentRecipeIds,
                POST_LIMIT,
                CANDIDATE_LIMIT,
                semanticQuery
        );
    }

    public List<MealPlanAiCandidate> loadContinuationMealPlanCandidates(UserResponse user, String goal, Set<Long> recentRecipeIds) {
        String semanticQuery = goal == null || goal.isBlank()
                ? null
                : "Tìm món ăn phù hợp mục tiêu: " + goal.trim();
        return loadCandidateRecipes(
                user,
                goal,
                Set.of(),
                recentRecipeIds == null ? Set.of() : recentRecipeIds,
                POST_LIMIT,
                CANDIDATE_LIMIT,
                semanticQuery
        );
    }

    public List<MealPlanAiCandidate> loadReplacementMealPlanCandidates(
            Long userId,
            UserResponse user,
            String goal,
            String reason,
            Set<Long> excludedRecipeIds,
            List<MealSlot> mealSlots,
            int replacementCount
    ) {
        int candidateLimit = Math.max(replacementCount + 3, CANDIDATE_LIMIT);
        String semanticQuery = reason == null || reason.isBlank()
                ? null
                : "Tìm món ăn thay thế. Yêu cầu: " + reason.trim()
                + ". Mục tiêu: " + normalize(goal)
                + ". Bữa ăn: " + (mealSlots == null ? "" : mealSlots.stream()
                .filter(Objects::nonNull)
                .map(Enum::name)
                .distinct()
                .collect(Collectors.joining(", ")));
        return loadCandidateRecipes(
                user,
                goal,
                excludedRecipeIds == null ? Set.of() : excludedRecipeIds,
                loadRecentRecipeIds(userId),
                candidateLimit,
                candidateLimit,
                semanticQuery
        );
    }

    private List<MealPlanAiCandidate> loadCandidateRecipes(
            UserResponse user,
            String goal,
            Set<Long> excludedRecipeIds,
            Set<Long> recentRecipeIds,
            int postLimit,
            int candidateLimit,
            String semanticQuery
    ) {
        List<Post> approvedPosts = postRepository.findByStatusAndIsDeletedFalse(PostStatus.APPROVED);

        List<String> allergies = normalizeList(user != null ? user.getAllergies() : List.of());
        List<String> eatingPreferences = normalizeList(user != null ? user.getEatingPreferences() : List.of());
        List<String> dietaryPreferences = normalizeList(user != null ? user.getDietaryPreferences() : List.of());
        List<String> healthConditions = normalizeList(user != null ? user.getHealthConditions() : List.of());
        String userCity = user != null && user.getAddress() != null && user.getAddress().get("city") != null
                ? normalize(user.getAddress().get("city").asText())
                : "";
        List<Long> semanticPostIds;
        List<Post> preFilteredPosts = approvedPosts.stream()
                .filter(post -> post.getRecipeId() != null)
                .filter(post -> !excludedRecipeIds.contains(post.getRecipeId()))
                .filter(post -> !violatesAllergiesByKeywords(post, allergies))
                .toList();
        // log.info("Meal plan candidates after hard filter recipeIds={}", toRecipeIds(preFilteredPosts));

        if (semanticQuery != null && !semanticQuery.isBlank()) {
            semanticPostIds = chromaRagService.retrieveTopKSimilarPostIds(
                    preFilteredPosts.stream()
                            .map(Post::getId)
                            .toList(),
                    semanticQuery,
                    List.of(),
                    SEMANTIC_LIMIT
            );
            if (semanticPostIds.isEmpty()) {
                log.warn("Meal plan candidate fallback: no Chroma matches, using default candidate ranking");
            }
        } else {
            semanticPostIds = List.of();
        }
        log.info("Meal plan candidates from Chroma recipeIds={}", toRecipeIdsByPostIds(preFilteredPosts, semanticPostIds));

        Set<Long> semanticPostIdSet = new HashSet<>(semanticPostIds);
        List<Post> scoringPosts = semanticPostIdSet.isEmpty()
                ? preFilteredPosts
                : preFilteredPosts.stream()
                .filter(post -> semanticPostIdSet.contains(post.getId()))
                .toList();

        Map<Long, Integer> fastScores = scoringPosts.stream()
                .collect(Collectors.toMap(
                        Post::getId,
                        post -> scorePostBeforeNutrition(
                                post,
                                semanticPostIds,
                                eatingPreferences,
                                dietaryPreferences,
                                userCity,
                                recentRecipeIds
                        )
                ));

        List<Post> candidatePosts = scoringPosts.stream()
                .sorted(Comparator.comparingInt((Post post) ->
                        fastScores.getOrDefault(post.getId(), 0)
                ).reversed())
                .limit(postLimit)
                .toList();
        log.info("Meal plan candidates after fast score recipeIds={}", toRecipeIds(candidatePosts));

        Map<Long, NutritionAnalysisResponse> nutritionByRecipeId = fetchNutritionByRecipeIds(candidatePosts);
        List<MealPlanAiCandidate> nutritionCandidates = candidatePosts.stream()
                .map(post -> toCandidateRecipe(post, nutritionByRecipeId.get(post.getRecipeId())))
                .filter(Objects::nonNull)
                .toList();
        log.info("Meal plan candidates after nutrition fetch recipeIds={}", toCandidateRecipeIds(nutritionCandidates));

        List<MealPlanAiCandidate> finalCandidates = nutritionCandidates.stream()
                .sorted(Comparator.comparingInt((MealPlanAiCandidate candidate) ->
                        fastScores.getOrDefault(candidate.getPostId(), 0)
                                + mealPlanNutritionScoringService.score(candidate, goal, healthConditions)
                ).reversed())
                .limit(candidateLimit)
                .toList();
        log.info("Meal plan final candidates recipeIds={}", toCandidateRecipeIds(finalCandidates));
        return finalCandidates;
    }

    private Map<Long, NutritionAnalysisResponse> fetchNutritionByRecipeIds(List<Post> posts) {
        List<Long> recipeIds = posts.stream()
                .map(Post::getRecipeId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (recipeIds.isEmpty()) {
            return Map.of();
        }

        try {
            Map<Long, NutritionAnalysisResponse> data = recipeClient.getNutritionByRecipeIds(recipeIds, false).getData();
            return data == null ? Map.of() : data;
        } catch (Exception e) {
            log.warn("Meal plan batch nutrition fetch failed: {}", e.getMessage());
            return Map.of();
        }
    }

    private MealPlanAiCandidate toCandidateRecipe(Post post, NutritionAnalysisResponse nutrition) {
        if (nutrition == null) {
            log.warn("Skip recipeId={} due to missing nutrition in batch response", post.getRecipeId());
            return null;
        }
        return mealPlanAiMapper.toCandidate(post, nutrition);
    }

    private boolean violatesAllergiesByKeywords(Post post, List<String> allergies) {
        if (allergies.isEmpty() || post.getRecipeIngredientKeywords() == null || post.getRecipeIngredientKeywords().isEmpty()) {
            return false;
        }

        List<String> ingredientKeywords = post.getRecipeIngredientKeywords().stream()
                .map(this::normalize)
                .filter(value -> !value.isBlank())
                .toList();

        return hasAllergyMatch(ingredientKeywords, allergies);
    }

    private boolean hasAllergyMatch(List<String> ingredientNames, List<String> allergies) {
        return allergies.stream().anyMatch(allergy ->
                ingredientNames.stream().anyMatch(name -> name.contains(allergy) || allergy.contains(name))
        );
    }

    private int scorePostBeforeNutrition(
            Post post,
            List<Long> rankedSemanticPostIds,
            List<String> eatingPreferences,
            List<String> dietaryPreferences,
            String userCity,
            Set<Long> recentRecipeIds
    ) {
        return scoreSemantic(post, rankedSemanticPostIds)
                + scorePreferences(post, eatingPreferences, dietaryPreferences, userCity)
                + (post.getRecipeId() != null && recentRecipeIds.contains(post.getRecipeId()) ? -30 : 0);
    }

    private int scoreSemantic(Post post, List<Long> rankedSemanticPostIds) {
        if (post == null || rankedSemanticPostIds == null || rankedSemanticPostIds.isEmpty()) {
            return 0;
        }

        int index = rankedSemanticPostIds.indexOf(post.getId());
        return index < 0 ? 0 : Math.max(0, 50 - index * 2);
    }

    private int scorePreferences(
            Post post,
            List<String> eatingPreferences,
            List<String> dietaryPreferences,
            String userCity
    ) {
        int score = 0;

        List<String> ingredientKeywords = normalizeList(
                post.getRecipeIngredientKeywords() == null ? List.of() : new ArrayList<>(post.getRecipeIngredientKeywords())
        );
        List<String> recipeCategories = normalizeList(
                post.getRecipeCategories() == null ? List.of() : new ArrayList<>(post.getRecipeCategories())
        );
        String searchableText = normalize(post.getTitle()) + " " + normalize(post.getDescription());

        score += matchCount(ingredientKeywords, eatingPreferences) * 10;
        score += matchCount(recipeCategories, dietaryPreferences) * 10;

        if (eatingPreferences.stream().anyMatch(preference -> isTextMatch(searchableText, preference))) {
            score += 8;
        }
        if (dietaryPreferences.stream().anyMatch(preference -> isTextMatch(searchableText, preference))) {
            score += 8;
        }

        if (!userCity.isBlank() && isTextMatch(post.getRegion(), userCity)) {
            score += 5;
        }

        return Math.min(score, 30);
    }

    private int matchCount(List<String> sourceValues, List<String> preferences) {
        int matches = 0;
        for (String preference : preferences) {
            if (sourceValues.stream().anyMatch(value -> isTextMatch(value, preference))) {
                matches++;
            }
        }
        return matches;
    }

    private List<Long> toRecipeIds(List<Post> posts) {
        return posts.stream()
                .map(Post::getRecipeId)
                .filter(Objects::nonNull)
                .toList();
    }

    private List<Long> toRecipeIdsByPostIds(List<Post> posts, List<Long> postIds) {
        if (postIds == null || postIds.isEmpty()) {
            return List.of();
        }

        return postIds.stream()
                .map(postId -> posts.stream()
                        .filter(post -> Objects.equals(post.getId(), postId))
                        .map(Post::getRecipeId)
                        .filter(Objects::nonNull)
                        .findFirst()
                        .orElse(null))
                .filter(Objects::nonNull)
                .toList();
    }

    private List<Long> toCandidateRecipeIds(List<MealPlanAiCandidate> candidates) {
        return candidates.stream()
                .map(MealPlanAiCandidate::getRecipeId)
                .filter(Objects::nonNull)
                .toList();
    }

    private boolean isTextMatch(String text, String keyword) {
        String normalizedText = normalize(text);
        String normalizedKeyword = normalize(keyword);
        return !normalizedText.isBlank()
                && !normalizedKeyword.isBlank()
                && normalizedText.contains(normalizedKeyword);
    }

    public Set<Long> loadRecentRecipeIds(Long userId) {
        if (userId == null) {
            return Set.of();
        }

        return mealPlanRepository.findByUserId(userId)
                .map(MealPlan::getId)
                .map(mealPlanItemRepository::findAllByMealPlanIdOrderByPlanDateAscMealSlotAscItemOrderAsc)
                .orElse(List.of())
                .stream()
                .map(MealPlanItem::getRecipeId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(HashSet::new));
    }

    private List<String> normalizeList(List<String> values) {
        if (values == null) {
            return List.of();
        }
        return values.stream()
                .filter(Objects::nonNull)
                .map(this::normalize)
                .filter(value -> !value.isBlank())
                .distinct()
                .toList();
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
