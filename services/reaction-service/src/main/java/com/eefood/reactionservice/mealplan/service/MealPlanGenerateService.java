package com.eefood.reactionservice.mealplan.service;

import com.eefood.reactionservice.dto.response.UserBodyMetricsResponse;
import com.eefood.reactionservice.dto.response.UserResponse;
import com.eefood.reactionservice.enums.ErrorMessage;
import com.eefood.reactionservice.enums.PostStatus;
import com.eefood.reactionservice.exception.ExceptionUtil;
import com.eefood.reactionservice.mealplan.dto.ai.GeneratedMealItem;
import com.eefood.reactionservice.mealplan.dto.ai.MealPlanAiCandidate;
import com.eefood.reactionservice.mealplan.dto.request.MealPlanGenerateRequest;
import com.eefood.reactionservice.mealplan.dto.response.MealPlanResponse;
import com.eefood.reactionservice.mealplan.dto.response.NutritionAnalysisResponse;
import com.eefood.reactionservice.mealplan.enums.MealPlanItemSource;
import com.eefood.reactionservice.mealplan.enums.MealPlanItemStatus;
import com.eefood.reactionservice.mealplan.enums.MealSlot;
import com.eefood.reactionservice.mealplan.model.MealPlan;
import com.eefood.reactionservice.mealplan.model.MealPlanItem;
import com.eefood.reactionservice.mealplan.repo.MealPlanItemIngredientRepository;
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
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Service
@RequiredArgsConstructor
@Slf4j
public class MealPlanGenerateService {

    private static final int MAX_GENERATE_DAYS = 5;
    private static final int DEFAULT_DAYS = 3;
    private static final int INITIAL_POST_LIMIT = 30;
    private static final int CANDIDATE_LIMIT = 12;
    private static final List<MealSlot> DEFAULT_SLOTS = List.of(MealSlot.BREAKFAST, MealSlot.LUNCH, MealSlot.DINNER);

    private final MealPlanRepository mealPlanRepository;
    private final MealPlanItemRepository mealPlanItemRepository;
    private final MealPlanItemIngredientRepository mealPlanItemIngredientRepository;
    private final PostRepository postRepository;
    private final IamClient iamClient;
    private final RecipeClient recipeClient;
    private final MealPlanService mealPlanService;
    private final MealPlanAiPlannerService mealPlanAiPlannerService;
    private final Executor applicationTaskExecutor;

    @Transactional
    public MealPlanResponse generateInitialMealPlan(Long userId, MealPlanGenerateRequest request) {
        // lấy user + candidate, generate plan, rồi thay item hiện tại của plan.
        validateGenerateRequest(userId, request);

        UserResponse user = iamClient.getUserById(userId).getData();
        UserBodyMetricsResponse bodyMetrics = iamClient.getUserBodyMetrics(userId).getData();
        List<MealPlanAiCandidate> candidates = loadCandidateRecipes(userId, user, bodyMetrics, request.getGoal());

        if (candidates.isEmpty()) {
            throw ExceptionUtil.notFound(ErrorMessage.RECIPE_NOT_FOUND);
        }

        List<GeneratedMealItem> generatedItems = mealPlanAiPlannerService.generatePlan(
                user,
                bodyMetrics,
                request,
                candidates,
                resolveDays(request)
        );
        if (generatedItems.isEmpty()) {
            generatedItems = fallbackGenerate(request, candidates);
        }

        MealPlan mealPlan = mealPlanRepository.findByUserId(userId)
                .orElseGet(() -> MealPlan.builder().userId(userId).build());

        mealPlan.setGoal(request.getGoal());
        mealPlan.setStartDate(request.getStartDate());
        mealPlan.setEndDate(request.getStartDate().plusDays(resolveDays(request) - 1L));
        mealPlan.setNote("kế hoạch nấu ăn");
        mealPlan.setUserHealthNote(buildUserHealthNote(user, bodyMetrics));

        MealPlan savedMealPlan = mealPlanRepository.save(mealPlan);

        deleteIngredientsByMealPlanId(savedMealPlan.getId());
        mealPlanItemRepository.deleteAllByMealPlanId(savedMealPlan.getId());
        saveGeneratedItems(savedMealPlan.getId(), generatedItems);

        return mealPlanService.getCurrentMealPlan(userId);
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
        List<MealPlanAiCandidate> candidates = loadCandidateRecipes(userId, user, bodyMetrics, mealPlan.getGoal());

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

        deleteIngredientsByMealPlanIdAndDateRange(savedMealPlan.getId(), nextStartDate, nextEndDate);
        mealPlanItemRepository.deleteAllByMealPlanIdAndPlanDateBetween(savedMealPlan.getId(), nextStartDate, nextEndDate);
        saveGeneratedItems(savedMealPlan.getId(), generatedItems);

        return mealPlanService.getCurrentMealPlan(userId);
    }

    private List<MealPlanAiCandidate> loadCandidateRecipes(
            Long userId,
            UserResponse user,
            UserBodyMetricsResponse bodyMetrics,
            String goal
    ) {
        // Lấy các post đã duyệt, rồi loại món vi phạm dị ứng.
        List<Post> approvedPosts = postRepository.findByStatusAndIsDeletedFalse(
                PostStatus.APPROVED,
                PageRequest.of(0, 100)
        );

        List<String> allergies = normalizeList(user != null ? user.getAllergies() : List.of());
        List<String> eatingPreferences = normalizeList(user != null ? user.getEatingPreferences() : List.of());
        List<String> dietaryPreferences = normalizeList(user != null ? user.getDietaryPreferences() : List.of());
        // Dùng city để cộng nhẹ cho món cùng vùng miền.
        String userCity = user != null && user.getAddress() != null && user.getAddress().get("city") != null
                ? normalize(user.getAddress().get("city").asText())
                : "";
        Set<Long> recentRecipeIds = loadRecentRecipeIds(userId);

        // Chọn pool ban đầu: lọc cứng theo recipeId/dị ứng, rồi sort theo score sơ bộ.
        List<Post> candidatePosts = approvedPosts.stream()
                .filter(post -> post.getRecipeId() != null)
                .filter(post -> !violatesAllergiesByKeywords(post, allergies))
                // Score sơ bộ = goal score + preference score - repeat penalty.
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

        // Chỉ gọi nutrition cho pool đã qua vòng lọc đầu để giảm thời gian chờ.
        List<CompletableFuture<MealPlanAiCandidate>> futures = candidatePosts.stream()
                .map(post -> CompletableFuture.supplyAsync(
                        () -> toCandidateRecipe(post),
                        applicationTaskExecutor
                ))
                .toList();

        CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).join();

        // Giữ lại tối đa CANDIDATE_LIMIT candidate cuối cùng sau khi gọi nutrition.
        return futures.stream()
                .map(CompletableFuture::join)
                .filter(Objects::nonNull)
                .filter(candidate -> !violatesAllergies(candidate, allergies))
                .limit(CANDIDATE_LIMIT)
                .toList();
    }

    private MealPlanAiCandidate toCandidateRecipe(Post post) {
        // Chuyển post đã duyệt thành candidate recipe kèm snapshot dinh dưỡng.
        try {
            NutritionAnalysisResponse nutrition = recipeClient.getNutritionByRecipeId(post.getRecipeId(), false).getData();
            if (nutrition == null) {
                return null;
            }

            return MealPlanAiCandidate.builder()
                    .recipeId(post.getRecipeId())
                    .postId(post.getId())
                    .title(post.getTitle())
                    .description(post.getDescription())
                    .imageUrl(post.getImageUrl())
                    .region(post.getRegion())
                    .prepTime(post.getPrepTime())
                    .cookTime(post.getCookTime())
                    .difficulty(post.getDifficulty() != null ? post.getDifficulty().name() : null)
                    .ingredientKeywords(post.getRecipeIngredientKeywords() != null
                            ? new ArrayList<>(post.getRecipeIngredientKeywords()) : List.of())
                    .nutrition(nutrition)
                    .build();
        } catch (Exception e) {
            log.warn("Skip recipeId={} due to nutrition fetch failure: {}", post.getRecipeId(), e.getMessage());
            return null;
        }
    }


    private List<GeneratedMealItem> fallbackGenerate(MealPlanGenerateRequest request, List<MealPlanAiCandidate> candidates) {
        // Fallback cố định: xoay vòng candidate theo ngày và theo bữa.
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

    private MealPlanItem toMealPlanItem(Long mealPlanId, GeneratedMealItem generatedItem) {
        // Lưu kết quả AI thành MealPlanItem với snapshot dinh dưỡng từ recipe.
        MealPlanAiCandidate candidate = generatedItem.getCandidate();
        NutritionAnalysisResponse nutrition = candidate.getNutrition();

        return MealPlanItem.builder()
                .mealPlanId(mealPlanId)
                .planDate(generatedItem.getPlanDate())
                .mealSlot(generatedItem.getMealSlot())
                .itemOrder(generatedItem.getItemOrder())
                .itemSource(MealPlanItemSource.RECIPE)
                .recipeId(candidate.getRecipeId())
                .postId(candidate.getPostId())
                .plannedServings(generatedItem.getServings())
                .actualServings(null)
                .status(MealPlanItemStatus.PLANNED)
                .recipeTitle(candidate.getTitle())
                .imageUrl(candidate.getImageUrl())
                .calories(toBigDecimal(nutrition.getTotalCalories()))
                .protein(toBigDecimal(nutrition.getTotalProtein()))
                .carbs(toBigDecimal(nutrition.getTotalCarb()))
                .fat(toBigDecimal(nutrition.getTotalFat()))
                .fiber(toBigDecimal(nutrition.getTotalFiber()))
                .sugar(toBigDecimal(nutrition.getTotalSugar()))
                .calcium(toBigDecimal(nutrition.getTotalCalcium()))
                .sodium(toBigDecimal(nutrition.getTotalSodium()))
                .note(generatedItem.getNote())
                .build();
    }

    private void saveGeneratedItems(Long mealPlanId, List<GeneratedMealItem> generatedItems) {
        mealPlanItemRepository.saveAll(
                generatedItems.stream()
                        .map(item -> toMealPlanItem(mealPlanId, item))
                        .toList()
        );
    }

    private void deleteIngredientsByMealPlanId(Long mealPlanId) {
        // Dọn child rows trước khi xóa item để tránh orphan data hoặc lỗi FK.
        List<Long> itemIds = mealPlanItemRepository.findAllByMealPlanIdOrderByPlanDateAscMealSlotAscItemOrderAsc(mealPlanId).stream()
                .map(MealPlanItem::getId)
                .toList();
        if (!itemIds.isEmpty()) {
            mealPlanItemIngredientRepository.deleteAllByMealPlanItemIdIn(itemIds);
        }
    }

    private void deleteIngredientsByMealPlanIdAndDateRange(Long mealPlanId, LocalDate startDate, LocalDate endDate) {
        // Chỉ dọn ingredients của đoạn ngày sắp bị replace trong flow continue.
        List<Long> itemIds = mealPlanItemRepository.findAllByMealPlanIdOrderByPlanDateAscMealSlotAscItemOrderAsc(mealPlanId).stream()
                .filter(item -> item.getPlanDate() != null)
                .filter(item -> !item.getPlanDate().isBefore(startDate) && !item.getPlanDate().isAfter(endDate))
                .map(MealPlanItem::getId)
                .toList();
        if (!itemIds.isEmpty()) {
            mealPlanItemIngredientRepository.deleteAllByMealPlanItemIdIn(itemIds);
        }
    }

    private boolean violatesAllergies(MealPlanAiCandidate candidate, List<String> allergies) {
        // Kiểm tra dị ứng bằng ingredientDetails sau khi đã lấy nutrition.
        // Rule cứng: loại candidate nếu tên nguyên liệu trùng keyword dị ứng.
        if (allergies.isEmpty() || candidate.getNutrition() == null || candidate.getNutrition().getIngredientDetails() == null) {
            return false;
        }

        List<String> ingredientNames = candidate.getNutrition().getIngredientDetails().stream()
                .map(detail -> normalize(detail.getIngredientName()))
                .toList();

        return hasAllergyMatch(ingredientNames, allergies);
    }

    private boolean violatesAllergiesByKeywords(Post post, List<String> allergies) {
        // Kiểm tra dị ứng sớm bằng recipeIngredientKeywords có sẵn trên post.
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
        // Match mềm theo contains để bắt các trường hợp keyword không trùng tuyệt đối.
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
        // Score vòng đầu chỉ dùng dữ liệu nhẹ của Post, chưa dùng nutrition.
        return scoreGoal(post, goal)
                + scorePreferences(post, eatingPreferences, dietaryPreferences, userCity)
                + (post.getRecipeId() != null && recentRecipeIds.contains(post.getRecipeId()) ? -30 : 0);
    }

    private int scoreGoal(Post post, String goal) {
        String normalizedGoal = normalize(goal);
        if (normalizedGoal.isBlank()) {
            return 0;
        }

        // Gom title, description, category và ingredient keyword thành một chuỗi để so goal.
        String searchableText = String.join(" ",
                normalize(post.getTitle()),
                normalize(post.getDescription()),
                String.join(" ", normalizeList(post.getRecipeCategories() == null ? List.of() : new ArrayList<>(post.getRecipeCategories()))),
                String.join(" ", normalizeList(post.getRecipeIngredientKeywords() == null ? List.of() : new ArrayList<>(post.getRecipeIngredientKeywords())))
        );

        List<String> positiveKeywords = List.of();
        List<String> negativeKeywords = List.of();

        if (List.of("giảm cân", "giam can", "eat clean", "healthy", "an kieng", "ăn kiêng")
                .stream()
                .anyMatch(keyword -> isTextMatch(normalizedGoal, keyword))) {
            positiveKeywords = List.of("salad", "luộc", "luoc", "hấp", "hap", "rau", "healthy", "clean", "canh");
            negativeKeywords = List.of("chiên", "chien", "rán", "ran", "bánh ngọt", "banh ngot", "ngọt", "dessert");
        } else if (List.of("tăng cơ", "tang co", "protein", "muscle")
                .stream()
                .anyMatch(keyword -> isTextMatch(normalizedGoal, keyword))) {
            positiveKeywords = List.of("gà", "ga", "bò", "bo", "trứng", "trung", "cá", "ca", "đậu", "dau", "protein");
        } else if (List.of("ít đường", "it duong", "tiểu đường", "tieu duong", "low sugar")
                .stream()
                .anyMatch(keyword -> isTextMatch(normalizedGoal, keyword))) {
            positiveKeywords = List.of("ít đường", "it duong", "không đường", "khong duong", "healthy");
            negativeKeywords = List.of("chè", "che", "bánh", "banh", "kẹo", "keo", "ngọt", "dessert");
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

        // Goal score: 2 keyword tích cực mới bù được 1 keyword tiêu cực.
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
        // Preference score ưu tiên ingredient keyword, category, rồi mới tới text tự do.
        int score = 0;

        List<String> ingredientKeywords = normalizeList(
                post.getRecipeIngredientKeywords() == null ? List.of() : new ArrayList<>(post.getRecipeIngredientKeywords())
        );
        List<String> recipeCategories = normalizeList(
                post.getRecipeCategories() == null ? List.of() : new ArrayList<>(post.getRecipeCategories())
        );
        String searchableText = normalize(post.getTitle()) + " " + normalize(post.getDescription());

        // Mỗi match nguyên liệu/chế độ ăn được cộng 10 điểm.
        score += matchCount(ingredientKeywords, eatingPreferences) * 10;
        score += matchCount(recipeCategories, dietaryPreferences) * 10;

        // Text title/description chỉ cần có ít nhất 1 match là cộng điểm.
        if (eatingPreferences.stream().anyMatch(preference -> isTextMatch(searchableText, preference))) {
            score += 8;
        }
        if (dietaryPreferences.stream().anyMatch(preference -> isTextMatch(searchableText, preference))) {
            score += 8;
        }

        // Cùng vùng miền với user 
        if (!userCity.isBlank() && isTextMatch(post.getRegion(), userCity)) {
            score += 5;
        }

        // giới hạn 30đ
        return Math.min(score, 30);
    }

    private int matchCount(List<String> sourceValues, List<String> preferences) {
        // Mỗi preference khớp với tag/category của post được tính là một match.
        int matches = 0;
        for (String preference : preferences) {
            if (sourceValues.stream().anyMatch(value -> isTextMatch(value, preference))) {
                matches++;
            }
        }
        return matches;
    }

    private boolean isTextMatch(String text, String keyword) {
        // Match mềm theo contains để tận dụng dữ liệu text chưa chuẩn hóa tuyệt đối.
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
                .collect(java.util.stream.Collectors.toCollection(HashSet::new));
    }

    private String buildUserHealthNote(UserResponse user, UserBodyMetricsResponse bodyMetrics) {
        // Lưu ghi chú sức khỏe ngắn gọn vào meal plan để hiển thị hoặc debug.
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
        // Mặc định sinh plan ngắn và luôn chặn ở giới hạn business.
        int days = request.getDays() == null || request.getDays() <= 0 ? DEFAULT_DAYS : request.getDays();
        return Math.min(days, MAX_GENERATE_DAYS);
    }

    private int resolveDays(Integer days) {
        int resolvedDays = days == null || days <= 0 ? DEFAULT_DAYS : days;
        return Math.min(resolvedDays, MAX_GENERATE_DAYS);
    }

    private void validateGenerateRequest(Long userId, MealPlanGenerateRequest request) {
        // Validate request cơ bản trước khi gọi xuống các service khác.
        if (userId == null || request == null || request.getStartDate() == null || request.getGoal() == null || request.getGoal().isBlank()) {
            throw ExceptionUtil.badRequest(ErrorMessage.INVALID_REQUEST);
        }

        if (request.getDays() != null && (request.getDays() <= 0 || request.getDays() > MAX_GENERATE_DAYS)) {
            throw ExceptionUtil.badRequest(ErrorMessage.INVALID_REQUEST);
        }
    }

    private BigDecimal toBigDecimal(Double value) {
        return value == null ? null : BigDecimal.valueOf(value);
    }
}
