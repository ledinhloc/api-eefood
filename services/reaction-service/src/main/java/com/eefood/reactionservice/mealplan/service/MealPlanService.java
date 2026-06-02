package com.eefood.reactionservice.mealplan.service;

import com.eefood.reactionservice.dto.response.UserBodyMetricsResponse;
import com.eefood.reactionservice.dto.response.UserResponse;
import com.eefood.reactionservice.enums.ErrorMessage;
import com.eefood.reactionservice.enums.PostStatus;
import com.eefood.reactionservice.exception.ExceptionUtil;
import com.eefood.reactionservice.mealplan.dto.ai.GeneratedMealItem;
import com.eefood.reactionservice.mealplan.dto.ai.MealPlanAiCandidate;
import com.eefood.reactionservice.mealplan.dto.request.MealPlanGenerateRequest;
import com.eefood.reactionservice.mealplan.dto.request.MealPlanUpsertRequest;
import com.eefood.reactionservice.mealplan.dto.response.MealPlanDailySummaryResponse;
import com.eefood.reactionservice.mealplan.dto.response.MealPlanItemResponse;
import com.eefood.reactionservice.mealplan.dto.response.MealPlanResponse;
import com.eefood.reactionservice.mealplan.dto.response.NutritionAnalysisResponse;
import com.eefood.reactionservice.mealplan.enums.MealSlot;
import com.eefood.reactionservice.mealplan.mapper.MealPlanAiMapper;
import com.eefood.reactionservice.mealplan.mapper.MealPlanItemMapper;
import com.eefood.reactionservice.mealplan.mapper.MealPlanMapper;
import com.eefood.reactionservice.mealplan.model.MealPlan;
import com.eefood.reactionservice.mealplan.model.MealPlanItem;
import com.eefood.reactionservice.mealplan.repo.MealPlanItemRepository;
import com.eefood.reactionservice.mealplan.repo.MealPlanRepository;
import com.eefood.reactionservice.model.Post;
import com.eefood.reactionservice.repository.httpclient.IamClient;
import com.eefood.reactionservice.repository.httpclient.RecipeClient;
import com.eefood.reactionservice.repository.post.PostRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MealPlanService {
    private static final int MAX_GENERATE_DAYS = 5;
    private static final int DEFAULT_DAYS = 3;
    private static final int INITIAL_POST_LIMIT = 30;
    private static final int CANDIDATE_LIMIT = 12;
    private static final List<MealSlot> DEFAULT_SLOTS = List.of(MealSlot.BREAKFAST, MealSlot.LUNCH, MealSlot.DINNER);

    private final MealPlanRepository mealPlanRepository;
    private final MealPlanItemRepository mealPlanItemRepository;
    private final PostRepository postRepository;
    private final IamClient iamClient;
    private final RecipeClient recipeClient;
    private final MealPlanMapper mealPlanMapper;
    private final MealPlanItemMapper mealPlanItemMapper;
    private final MealPlanAiMapper mealPlanAiMapper;
    private final MealPlanAiPlannerService mealPlanAiPlannerService;
    private final MealPlanIngredientService mealPlanIngredientService;
    private final Executor applicationTaskExecutor;

    @Transactional
    public MealPlanResponse upsertCurrentMealPlan(Long userId, MealPlanUpsertRequest request) {
        if (userId == null || request == null) {
            throw ExceptionUtil.badRequest(ErrorMessage.INVALID_REQUEST);
        }

        if (request.getStartDate() == null || request.getEndDate() == null) {
            throw ExceptionUtil.badRequest(ErrorMessage.INVALID_REQUEST);
        }

        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw ExceptionUtil.badRequest(ErrorMessage.INVALID_REQUEST);
        }

        MealPlan mealPlan = mealPlanRepository.findByUserId(userId)
                .orElseGet(() -> MealPlan.builder().userId(userId).build());

        mealPlanMapper.updateFromRequest(request, mealPlan);

        MealPlan savedMealPlan = mealPlanRepository.save(mealPlan);
        return mealPlanMapper.toResponse(savedMealPlan);
    }

    @Transactional
    public MealPlanResponse getCurrentMealPlan(Long userId) {
        if (userId == null) {
            throw ExceptionUtil.badRequest(ErrorMessage.INVALID_REQUEST);
        }

        return mealPlanRepository.findByUserId(userId)
                .map(mealPlanMapper::toResponse)
                .orElse(null);
    }

    @Transactional
    public MealPlanResponse generateInitialMealPlan(Long userId, MealPlanGenerateRequest request) {
        // Lay user va candidate, generate plan, roi thay item hien tai cua plan.
        if (userId == null || request == null || request.getStartDate() == null || request.getGoal() == null || request.getGoal().isBlank()) {
            throw ExceptionUtil.badRequest(ErrorMessage.INVALID_REQUEST);
        }

        if (request.getDays() != null && (request.getDays() <= 0 || request.getDays() > MAX_GENERATE_DAYS)) {
            throw ExceptionUtil.badRequest(ErrorMessage.INVALID_REQUEST);
        }

        UserResponse user = iamClient.getUserById(userId).getData();
        UserBodyMetricsResponse bodyMetrics = iamClient.getUserBodyMetrics(userId).getData();
        List<MealPlanAiCandidate> candidates = loadCandidateRecipes(userId, user, request.getGoal());

        if (candidates.isEmpty()) {
            throw ExceptionUtil.notFound(ErrorMessage.RECIPE_NOT_FOUND);
        }

        int resolvedDays = resolveDays(request);

        List<GeneratedMealItem> generatedItems = mealPlanAiPlannerService.generatePlan(
                user,
                bodyMetrics,
                request,
                candidates,
                resolvedDays
        );
        if (generatedItems.isEmpty()) {
            generatedItems = fallbackGenerate(request, candidates);
        }

        MealPlan mealPlan = mealPlanRepository.findByUserId(userId)
                .orElseGet(() -> MealPlan.builder().userId(userId).build());

        mealPlan.setGoal(request.getGoal());
        mealPlan.setStartDate(request.getStartDate());
        mealPlan.setEndDate(request.getStartDate().plusDays(resolvedDays - 1L));
        mealPlan.setNote("Kế hoạch nấu ăn");
        mealPlan.setUserHealthNote(buildUserHealthNote(user, bodyMetrics));

        MealPlan savedMealPlan = mealPlanRepository.save(mealPlan);

        mealPlanIngredientService.deleteIngredientsByMealPlanId(savedMealPlan.getId());
        mealPlanItemRepository.deleteAllByMealPlanId(savedMealPlan.getId());
        saveGeneratedItems(savedMealPlan.getId(), generatedItems);

        return getCurrentMealPlan(userId);
    }

    @Transactional
    public MealPlanResponse continueMealPlan(Long userId, LocalDate startDate, Integer days) {
        if (userId == null) {
            throw ExceptionUtil.badRequest(ErrorMessage.INVALID_REQUEST);
        }

        if (days != null && (days <= 0 || days > MAX_GENERATE_DAYS)) {
            throw ExceptionUtil.badRequest(ErrorMessage.INVALID_REQUEST);
        }

        MealPlan mealPlan = mealPlanRepository.findByUserId(userId)
                .orElseThrow(() -> ExceptionUtil.notFound(ErrorMessage.MEAL_PLAN_NOT_FOUND));

        if (mealPlan.getStartDate() == null || mealPlan.getEndDate() == null
                || mealPlan.getGoal() == null || mealPlan.getGoal().isBlank()) {
            throw ExceptionUtil.badRequest(ErrorMessage.INVALID_REQUEST);
        }
        int resolvedDays = days == null ? DEFAULT_DAYS : days;

        LocalDate nextStartDate = startDate != null ? startDate : mealPlan.getEndDate().plusDays(1);
        LocalDate nextEndDate = nextStartDate.plusDays(resolvedDays - 1L);
        UserResponse user = iamClient.getUserById(userId).getData();
        UserBodyMetricsResponse bodyMetrics = iamClient.getUserBodyMetrics(userId).getData();
        List<MealPlanAiCandidate> candidates = loadCandidateRecipes(userId, user, mealPlan.getGoal());

        if (candidates.isEmpty()) {
            throw ExceptionUtil.notFound(ErrorMessage.RECIPE_NOT_FOUND);
        }

        MealPlanGenerateRequest continueRequest = MealPlanGenerateRequest.builder()
                .goal(mealPlan.getGoal())
                .startDate(nextStartDate)
                .days(resolvedDays)
                .build();

        List<GeneratedMealItem> generatedItems = mealPlanAiPlannerService.generatePlan(
                user,
                bodyMetrics,
                continueRequest,
                candidates,
                resolvedDays
        );
        if (generatedItems.isEmpty()) {
            generatedItems = fallbackGenerate(continueRequest, candidates);
        }

        mealPlan.setEndDate(nextEndDate);
        mealPlan.setNote("Generated by AI");
        mealPlan.setUserHealthNote(buildUserHealthNote(user, bodyMetrics));
        MealPlan savedMealPlan = mealPlanRepository.save(mealPlan);

        mealPlanIngredientService.deleteIngredientsByMealPlanIdAndDateRange(savedMealPlan.getId(), nextStartDate, nextEndDate);
        mealPlanItemRepository.deleteAllByMealPlanIdAndPlanDateBetween(savedMealPlan.getId(), nextStartDate, nextEndDate);
        saveGeneratedItems(savedMealPlan.getId(), generatedItems);

        return getCurrentMealPlan(userId);
    }

    @Transactional
    public List<MealPlanDailySummaryResponse> getDailySummaries(Long userId) {
        if (userId == null) {
            throw ExceptionUtil.badRequest(ErrorMessage.INVALID_REQUEST);
        }

        return mealPlanRepository.findByUserId(userId)
                .map(mealPlan -> {
                    // Gom tat ca item theo ngay va cong don dinh duong theo servings cua tung item.
                    Map<LocalDate, List<MealPlanItem>> itemsByDate = mealPlanItemRepository
                            .findAllByMealPlanIdOrderByPlanDateAscMealSlotAscItemOrderAsc(mealPlan.getId()).stream()
                            .filter(item -> item.getPlanDate() != null)
                            .collect(Collectors.groupingBy(
                                    MealPlanItem::getPlanDate,
                                    LinkedHashMap::new,
                                    Collectors.toList()
                            ));
                            // {
                            //   2026-03-28 -> [item1, item2, item3],
                            //   2026-03-29 -> [item4, item5]
                            // }


                    return itemsByDate.entrySet().stream()
                            .sorted(Map.Entry.comparingByKey(Comparator.naturalOrder()))
                            .map(entry -> toDailySummary(entry.getKey(), entry.getValue()))
                            .toList();
                })
                .orElse(List.of());
    }

    @Transactional
    public List<MealPlanItemResponse> getItemsByDate(Long userId, LocalDate planDate) {
        if (userId == null || planDate == null) {
            throw ExceptionUtil.badRequest(ErrorMessage.INVALID_REQUEST);
        }

        MealPlan mealPlan = mealPlanRepository.findByUserId(userId)
                .orElseThrow(() -> ExceptionUtil.notFound(ErrorMessage.MEAL_PLAN_NOT_FOUND));

        List<MealPlanItemResponse> responses = mealPlanItemRepository
                .findAllByMealPlanIdAndPlanDateOrderByMealSlotAscItemOrderAsc(mealPlan.getId(), planDate)
                .stream()
                .map(mealPlanItemMapper::toScaledResponse)
                .toList();

        mealPlanIngredientService.hydrateIngredients(responses);
        return responses;
    }

    private MealPlanDailySummaryResponse toDailySummary(LocalDate planDate, List<MealPlanItem> items) {
        // Uu tien actualServings neu da co, nguoc lai dung plannedServings de tinh tong theo ngay.
        BigDecimal calories = BigDecimal.ZERO;
        BigDecimal protein = BigDecimal.ZERO;
        BigDecimal carbs = BigDecimal.ZERO;
        BigDecimal fat = BigDecimal.ZERO;
        BigDecimal fiber = BigDecimal.ZERO;
        BigDecimal sugar = BigDecimal.ZERO;
        BigDecimal sodium = BigDecimal.ZERO;
        BigDecimal calcium = BigDecimal.ZERO;

        for (MealPlanItem item : items) {
            int servings = resolveServings(item);
            BigDecimal multiplier = BigDecimal.valueOf(servings);

            calories = calories.add(scale(item.getCalories(), multiplier));
            protein = protein.add(scale(item.getProtein(), multiplier));
            carbs = carbs.add(scale(item.getCarbs(), multiplier));
            fat = fat.add(scale(item.getFat(), multiplier));
            fiber = fiber.add(scale(item.getFiber(), multiplier));
            sugar = sugar.add(scale(item.getSugar(), multiplier));
            sodium = sodium.add(scale(item.getSodium(), multiplier));
            calcium = calcium.add(scale(item.getCalcium(), multiplier));
        }

        return MealPlanDailySummaryResponse.builder()
                .planDate(planDate)
                .calories(calories)
                .protein(protein)
                .carbs(carbs)
                .fat(fat)
                .fiber(fiber)
                .sugar(sugar)
                .sodium(sodium)
                .calcium(calcium)
                .build();
    }

    private int resolveServings(MealPlanItem item) {
        Integer servings = item.getActualServings() != null ? item.getActualServings() : item.getPlannedServings();
        return servings == null || servings <= 0 ? 1 : servings;
    }

    private BigDecimal scale(BigDecimal value, BigDecimal multiplier) {
        return value == null ? BigDecimal.ZERO : value.multiply(multiplier);
    }

    private List<MealPlanAiCandidate> loadCandidateRecipes(
            Long userId,
            UserResponse user,
            String goal
    ) {
        // Lay cac post da duyet, roi loai mon vi pham di ung.
        List<Post> approvedPosts = postRepository.findByStatusAndIsDeletedFalse(
                PostStatus.APPROVED,
                PageRequest.of(0, 100)
        );

        List<String> allergies = normalizeList(user != null ? user.getAllergies() : List.of());
        List<String> eatingPreferences = normalizeList(user != null ? user.getEatingPreferences() : List.of());
        List<String> dietaryPreferences = normalizeList(user != null ? user.getDietaryPreferences() : List.of());
        // Dung city de cong nhe cho mon cung vung mien.
        String userCity = user != null && user.getAddress() != null && user.getAddress().get("city") != null
                ? normalize(user.getAddress().get("city").asText())
                : "";
        Set<Long> recentRecipeIds = loadRecentRecipeIds(userId);

        // Chon pool ban dau: loc cung theo recipeId/di ung, roi sort theo score so bo.
        List<Post> candidatePosts = approvedPosts.stream()
                .filter(post -> post.getRecipeId() != null)
                .filter(post -> !violatesAllergiesByKeywords(post, allergies))
                // Điểm sơ bộ = điểm mục tiêu + điểm sở thích - điểm phạt món lặp.
                .sorted(Comparator.comparingInt((Post post) -> scorePostForInitialSelection(
                        post,
                        goal,
                        eatingPreferences,
                        dietaryPreferences,
                        userCity,
                        recentRecipeIds
                )).reversed())
                .limit(INITIAL_POST_LIMIT)
                .toList();

        // Chi goi nutrition cho pool da qua vong loc dau de giam thoi gian cho.
        List<CompletableFuture<MealPlanAiCandidate>> futures = candidatePosts.stream()
                .map(post -> CompletableFuture.supplyAsync(
                        () -> toCandidateRecipe(post),
                        applicationTaskExecutor
                ))
                .toList();

        CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).join();

        // Giu lai toi da CANDIDATE_LIMIT candidate cuoi cung sau khi goi nutrition.
        return futures.stream()
                .map(CompletableFuture::join)
                .filter(Objects::nonNull)
                .limit(CANDIDATE_LIMIT)
                .toList();
    }

    private MealPlanAiCandidate toCandidateRecipe(Post post) {
        // Chuyen post da duyet thanh candidate recipe kem snapshot dinh duong.
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

    private List<GeneratedMealItem> fallbackGenerate(MealPlanGenerateRequest request, List<MealPlanAiCandidate> candidates) {
        // Fallback co dinh: xoay vong candidate theo ngay va theo bua.
        List<GeneratedMealItem> generated = new ArrayList<>();
        int index = 0;
        int days = resolveDays(request);

        for (int day = 0; day < days; day++) {
            LocalDate planDate = request.getStartDate().plusDays(day);
            for (MealSlot mealSlot : DEFAULT_SLOTS) {
                MealPlanAiCandidate candidate = candidates.get(index % candidates.size());
                generated.add(GeneratedMealItem.builder()
                        .planDate(planDate)
                        .mealSlot(mealSlot)
                        .itemOrder(1)
                        .servings(1)
                        .note("Generated fallback")
                        .candidate(candidate)
                        .build());
                index++;
            }
        }
        return generated;
    }

    private void saveGeneratedItems(Long mealPlanId, List<GeneratedMealItem> generatedItems) {
        mealPlanItemRepository.saveAll(
                generatedItems.stream()
                        .map(item -> mealPlanItemMapper.toEntity(item, mealPlanId))
                        .toList()
        );
    }

    private boolean violatesAllergiesByKeywords(Post post, List<String> allergies) {
        // Kiem tra di ung som bang recipeIngredientKeywords co san tren post.
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
        // Match mem theo contains de bat cac truong hop keyword khong trung tuyet doi.
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
        // Chấm điểm vòng đầu chỉ dùng dữ liệu nhẹ của Post, chưa dùng nutrition.
        return scoreGoal(post, goal)
                + scorePreferences(post, eatingPreferences, dietaryPreferences, userCity)
                + (post.getRecipeId() != null && recentRecipeIds.contains(post.getRecipeId()) ? -30 : 0);
    }

    private int scoreGoal(Post post, String goal) {
        String normalizedGoal = normalize(goal);
        if (normalizedGoal.isBlank()) {
            return 0;
        }

        // Gom title, description, category va ingredient keyword thanh mot chuoi de so goal.
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

        // Goal score: 2 keyword tich cuc moi bu duoc 1 keyword tieu cuc.
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
        // Preference score uu tien ingredient keyword, category, roi moi toi text tu do.
        int score = 0;

        List<String> ingredientKeywords = normalizeList(
                post.getRecipeIngredientKeywords() == null ? List.of() : new ArrayList<>(post.getRecipeIngredientKeywords())
        );
        List<String> recipeCategories = normalizeList(
                post.getRecipeCategories() == null ? List.of() : new ArrayList<>(post.getRecipeCategories())
        );
        String searchableText = normalize(post.getTitle()) + " " + normalize(post.getDescription());

        // Moi match nguyen lieu/che do an duoc cong 10 diem.
        score += matchCount(ingredientKeywords, eatingPreferences) * 10;
        score += matchCount(recipeCategories, dietaryPreferences) * 10;

        // Text title/description chi can co it nhat 1 match la cong diem.
        if (eatingPreferences.stream().anyMatch(preference -> isTextMatch(searchableText, preference))) {
            score += 8;
        }
        if (dietaryPreferences.stream().anyMatch(preference -> isTextMatch(searchableText, preference))) {
            score += 8;
        }

        // Cung vung mien voi user.
        if (!userCity.isBlank() && isTextMatch(post.getRegion(), userCity)) {
            score += 5;
        }

        // Gioi han 30 diem.
        return Math.min(score, 30);
    }

    private int matchCount(List<String> sourceValues, List<String> preferences) {
        // Moi preference khop voi tag/category cua post duoc tinh la mot match.
        int matches = 0;
        for (String preference : preferences) {
            if (sourceValues.stream().anyMatch(value -> isTextMatch(value, preference))) {
                matches++;
            }
        }
        return matches;
    }

    private boolean isTextMatch(String text, String keyword) {
        // Match mem theo contains de tan dung du lieu text chua chuan hoa tuyet doi.
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

    private String buildUserHealthNote(UserResponse user, UserBodyMetricsResponse bodyMetrics) {
        // Luu ghi chu suc khoe ngan gon vao meal plan de hien thi hoac debug.
        if (user == null) {
            return null;
        }
        return "Allergies: " + normalizeList(user.getAllergies())
                + "; Eating preferences: " + normalizeList(user.getEatingPreferences())
                + "; Dietary preferences: " + normalizeList(user.getDietaryPreferences())
                + "; Activity level: " + normalize(user.getActivityLevel())
                + "; Height cm: " + (bodyMetrics != null ? bodyMetrics.getHeightCm() : null)
                + "; Weight kg: " + (bodyMetrics != null ? bodyMetrics.getWeightKg() : null)
                + "; Health conditions: " + normalizeList(user.getHealthConditions());
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

    private int resolveDays(MealPlanGenerateRequest request) {
        // Mac dinh sinh plan ngan va luon chan o gioi han business.
        int days = request.getDays() == null || request.getDays() <= 0 ? DEFAULT_DAYS : request.getDays();
        return Math.min(days, MAX_GENERATE_DAYS);
    }
}
