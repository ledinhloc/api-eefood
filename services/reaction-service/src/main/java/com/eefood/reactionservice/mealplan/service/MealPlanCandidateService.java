package com.eefood.reactionservice.mealplan.service;

import com.eefood.reactionservice.dto.response.UserResponse;
import com.eefood.reactionservice.enums.PostStatus;
import com.eefood.reactionservice.mealplan.dto.ai.MealPlanAiCandidate;
import com.eefood.reactionservice.mealplan.dto.response.NutritionAnalysisResponse;
import com.eefood.reactionservice.mealplan.mapper.MealPlanAiMapper;
import com.eefood.reactionservice.mealplan.model.MealPlan;
import com.eefood.reactionservice.mealplan.model.MealPlanItem;
import com.eefood.reactionservice.mealplan.repo.MealPlanItemRepository;
import com.eefood.reactionservice.mealplan.repo.MealPlanRepository;
import com.eefood.reactionservice.model.Post;
import com.eefood.reactionservice.repository.httpclient.RecipeClient;
import com.eefood.reactionservice.repository.post.PostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MealPlanCandidateService {
    private static final int POST_LIMIT = 20;
    private static final int CANDIDATE_LIMIT = 12;

    private final PostRepository postRepository;
    private final RecipeClient recipeClient;
    private final MealPlanRepository mealPlanRepository;
    private final MealPlanItemRepository mealPlanItemRepository;
    private final MealPlanAiMapper mealPlanAiMapper;
    private final MealPlanNutritionScoringService mealPlanNutritionScoringService;
    private final Executor applicationTaskExecutor;

    public List<MealPlanAiCandidate> loadCandidates(Long userId, UserResponse user, String goal) {
        List<Post> approvedPosts = postRepository.findByStatusAndIsDeletedFalse(PostStatus.APPROVED);

        List<String> allergies = normalizeList(user != null ? user.getAllergies() : List.of());
        List<String> eatingPreferences = normalizeList(user != null ? user.getEatingPreferences() : List.of());
        List<String> dietaryPreferences = normalizeList(user != null ? user.getDietaryPreferences() : List.of());
        List<String> healthConditions = normalizeList(user != null ? user.getHealthConditions() : List.of());
        String userCity = user != null && user.getAddress() != null && user.getAddress().get("city") != null
                ? normalize(user.getAddress().get("city").asText())
                : "";
        Set<Long> recentRecipeIds = loadRecentRecipeIds(userId);

        List<Post> candidatePosts = approvedPosts.stream()
                .filter(post -> post.getRecipeId() != null)
                .filter(post -> !violatesAllergiesByKeywords(post, allergies))
                .sorted(Comparator.comparingInt((Post post) -> scorePostForInitialSelection(
                        post,
                        goal,
                        eatingPreferences,
                        dietaryPreferences,
                        userCity,
                        recentRecipeIds
                )).reversed())
                .limit(POST_LIMIT)
                .toList();

        List<CompletableFuture<MealPlanAiCandidate>> futures = candidatePosts.stream()
                .map(post -> CompletableFuture.supplyAsync(
                        () -> toCandidateRecipe(post),
                        applicationTaskExecutor
                ))
                .toList();

        CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).join();

        return futures.stream()
                .map(CompletableFuture::join)
                .filter(Objects::nonNull)
                .sorted(Comparator.comparingInt((MealPlanAiCandidate candidate) ->
                        mealPlanNutritionScoringService.score(candidate, goal, healthConditions)
                ).reversed())
                .limit(CANDIDATE_LIMIT)
                .toList();
    }

    private MealPlanAiCandidate toCandidateRecipe(Post post) {
        try {
            NutritionAnalysisResponse nutrition = recipeClient.getNutritionByRecipeId(post.getRecipeId(), false).getData();
            if (nutrition == null) {
                return null;
            }

            return mealPlanAiMapper.toCandidate(post, nutrition);
        } catch (Exception e) {
            log.warn("Skip recipeId={} due to nutrition fetch failure: {}", post.getRecipeId(), e.getMessage());
            return null;
        }
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

    private int scorePostForInitialSelection(
            Post post,
            String goal,
            List<String> eatingPreferences,
            List<String> dietaryPreferences,
            String userCity,
            Set<Long> recentRecipeIds
    ) {
        return scoreGoal(post, goal)
                + scorePreferences(post, eatingPreferences, dietaryPreferences, userCity)
                + (post.getRecipeId() != null && recentRecipeIds.contains(post.getRecipeId()) ? -30 : 0);
    }

    private int scoreGoal(Post post, String goal) {
        String normalizedGoal = normalize(goal);
        if (normalizedGoal.isBlank()) {
            return 0;
        }

        String searchableText = String.join(" ",
                normalize(post.getTitle()),
                normalize(post.getDescription()),
                String.join(" ", normalizeList(post.getRecipeCategories() == null ? List.of() : new ArrayList<>(post.getRecipeCategories()))),
                String.join(" ", normalizeList(post.getRecipeIngredientKeywords() == null ? List.of() : new ArrayList<>(post.getRecipeIngredientKeywords())))
        );

        List<String> positiveKeywords = List.of();
        List<String> negativeKeywords = List.of();

        if (List.of("giảm cân", "giam can", "eat clean", "healthy", "ăn kiêng", "an kieng")
                .stream()
                .anyMatch(keyword -> isTextMatch(normalizedGoal, keyword))) {
            positiveKeywords = List.of("salad", "luộc", "luoc", "hấp", "hap", "rau", "healthy", "clean", "canh");
            negativeKeywords = List.of("chiên", "chien", "rán", "ran", "bánh ngọt", "banh ngot", "ngọt", "ngot", "dessert");
        } else if (List.of("tăng cơ", "tang co", "protein", "muscle")
                .stream()
                .anyMatch(keyword -> isTextMatch(normalizedGoal, keyword))) {
            positiveKeywords = List.of("gà", "ga", "bò", "bo", "trứng", "trung", "cá", "ca", "đậu", "dau", "protein");
        } else if (List.of("ít đường", "it duong", "tiểu đường", "tieu duong", "low sugar")
                .stream()
                .anyMatch(keyword -> isTextMatch(normalizedGoal, keyword))) {
            positiveKeywords = List.of("ít đường", "it duong", "không đường", "khong duong", "healthy");
            negativeKeywords = List.of("chè", "che", "bánh", "banh", "kẹo", "keo", "ngọt", "ngot", "dessert");
        }

        int positiveMatches = 0;
        for (String keyword : positiveKeywords) {
            if (isTextMatch(searchableText, keyword)) {
                positiveMatches++;
            }
        }

        int negativeMatches = 0;
        for (String keyword : negativeKeywords) {
            if (isTextMatch(searchableText, keyword)) {
                negativeMatches++;
            }
        }

        int weightedScore = positiveMatches - (negativeMatches * 2);

        if (weightedScore <= -2) {
            return -20;
        }
        if (weightedScore == -1 || weightedScore == 0) {
            return 0;
        }
        if (weightedScore == 1) {
            return 10;
        }
        if (weightedScore == 2) {
            return 20;
        }
        return 30;
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

    private boolean isTextMatch(String text, String keyword) {
        String normalizedText = normalize(text);
        String normalizedKeyword = normalize(keyword);
        return !normalizedText.isBlank()
                && !normalizedKeyword.isBlank()
                && (normalizedText.contains(normalizedKeyword) || normalizedKeyword.contains(normalizedText));
    }

    private Set<Long> loadRecentRecipeIds(Long userId) {
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
