package com.eefood.reactionservice.mealplan.service;

import com.eefood.reactionservice.dto.response.UserBodyMetricsResponse;
import com.eefood.reactionservice.dto.response.UserHeightResponse;
import com.eefood.reactionservice.dto.response.UserResponse;
import com.eefood.reactionservice.dto.response.UserWeightResponse;
import com.eefood.reactionservice.enums.ErrorMessage;
import com.eefood.reactionservice.exception.ExceptionUtil;
import com.eefood.reactionservice.mealplan.dto.ai.GeneratedMealItem;
import com.eefood.reactionservice.mealplan.dto.ai.MealPlanAiCandidate;
import com.eefood.reactionservice.mealplan.dto.request.MealPlanGenerateRequest;
import com.eefood.reactionservice.mealplan.dto.request.MealPlanUpsertRequest;
import com.eefood.reactionservice.mealplan.dto.response.MealPlanDailySummaryResponse;
import com.eefood.reactionservice.mealplan.dto.response.MealPlanItemResponse;
import com.eefood.reactionservice.mealplan.dto.response.MealPlanResponse;
import com.eefood.reactionservice.mealplan.enums.MealSlot;
import com.eefood.reactionservice.mealplan.mapper.MealPlanItemMapper;
import com.eefood.reactionservice.mealplan.mapper.MealPlanMapper;
import com.eefood.reactionservice.mealplan.model.MealPlan;
import com.eefood.reactionservice.mealplan.model.MealPlanItem;
import com.eefood.reactionservice.mealplan.repo.MealPlanItemRepository;
import com.eefood.reactionservice.mealplan.repo.MealPlanRepository;
import com.eefood.reactionservice.repository.httpclient.IamClient;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MealPlanService {
    private static final int MAX_GENERATE_DAYS = 5;
    private static final int DEFAULT_DAYS = 3;
    private static final List<MealSlot> DEFAULT_SLOTS = List.of(MealSlot.BREAKFAST, MealSlot.LUNCH, MealSlot.DINNER);

    private final MealPlanRepository mealPlanRepository;
    private final MealPlanItemRepository mealPlanItemRepository;
    private final IamClient iamClient;
    private final MealPlanMapper mealPlanMapper;
    private final MealPlanItemMapper mealPlanItemMapper;
    private final MealPlanAiPlannerService mealPlanAiPlannerService;
    private final MealPlanIngredientService mealPlanIngredientService;
    private final MealPlanCandidateService mealPlanCandidateService;

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
    public void deleteCurrentMealPlan(Long userId) {
        if (userId == null) {
            throw ExceptionUtil.badRequest(ErrorMessage.INVALID_REQUEST);
        }

        MealPlan mealPlan = mealPlanRepository.findByUserId(userId)
                .orElseThrow(() -> ExceptionUtil.notFound(ErrorMessage.MEAL_PLAN_NOT_FOUND));

        mealPlanRepository.delete(mealPlan);
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
        List<MealPlanAiCandidate> candidates = mealPlanCandidateService.loadCandidates(userId, user, request.getGoal());

        if (candidates.isEmpty()) {
            throw ExceptionUtil.notFound(ErrorMessage.RECIPE_NOT_FOUND);
        }

        int resolvedDays = resolveDays(request);

        List<GeneratedMealItem> generatedItems = mealPlanAiPlannerService.generateInitialMealPlan(
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
        if (!nextStartDate.isAfter(mealPlan.getEndDate())) {
            throw ExceptionUtil.badRequest(ErrorMessage.INVALID_REQUEST);
        }
        LocalDate nextEndDate = nextStartDate.plusDays(resolvedDays - 1L);
        UserResponse user = iamClient.getUserById(userId).getData();
        UserBodyMetricsResponse bodyMetrics = iamClient.getUserBodyMetrics(userId).getData();
        LocalDate historyEndDate = LocalDate.now().isBefore(mealPlan.getStartDate())
                ? mealPlan.getStartDate()
                : LocalDate.now();
        List<UserWeightResponse> weightHistory = iamClient
                .getUserWeights(userId, mealPlan.getStartDate(), historyEndDate)
                .getData();
        List<UserHeightResponse> heightHistory = iamClient
                .getUserHeights(userId, mealPlan.getStartDate(), historyEndDate)
                .getData();
        List<MealPlanAiCandidate> candidates = mealPlanCandidateService.loadCandidates(userId, user, mealPlan.getGoal());

        if (candidates.isEmpty()) {
            throw ExceptionUtil.notFound(ErrorMessage.RECIPE_NOT_FOUND);
        }

        MealPlanGenerateRequest continueRequest = MealPlanGenerateRequest.builder()
                .goal(mealPlan.getGoal())
                .startDate(nextStartDate)
                .days(resolvedDays)
                .build();

        List<GeneratedMealItem> generatedItems = mealPlanAiPlannerService.generateMealPlanContinuation(
                user,
                bodyMetrics,
                continueRequest,
                candidates,
                resolvedDays,
                weightHistory == null ? List.of() : weightHistory,
                heightHistory == null ? List.of() : heightHistory
        );
        if (generatedItems.isEmpty()) {
            generatedItems = fallbackGenerate(continueRequest, candidates);
        }

        mealPlan.setEndDate(nextEndDate);
        mealPlan.setNote("Kế hoạch nấu ăn");
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
                .findAllByMealPlanIdAndPlanDate(mealPlan.getId(), planDate)
                .stream()
                .sorted(Comparator
                        .comparing((MealPlanItem item) -> item.getMealSlot(), Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(MealPlanItem::getItemOrder, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(MealPlanItem::getId, Comparator.nullsLast(Long::compareTo)))
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
        List<MealPlanItem> savedItems = mealPlanItemRepository.saveAll(
                generatedItems.stream()
                        .map(item -> mealPlanItemMapper.toEntity(item, mealPlanId))
                        .toList()
        );

        for (int index = 0; index < savedItems.size(); index++) {
            // Tao snapshot nguyen lieu cho item do AI sinh ra.
            mealPlanIngredientService.replaceIngredientsFromRecipe(
                    savedItems.get(index).getId(),
                    savedItems.get(index).getRecipeId()
            );
        }
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
