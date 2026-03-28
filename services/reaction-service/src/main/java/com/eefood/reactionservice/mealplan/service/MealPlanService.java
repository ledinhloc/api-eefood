package com.eefood.reactionservice.mealplan.service;

import com.eefood.reactionservice.enums.ErrorMessage;
import com.eefood.reactionservice.exception.ExceptionUtil;
import com.eefood.reactionservice.mealplan.dto.request.MealPlanUpsertRequest;
import com.eefood.reactionservice.mealplan.dto.response.MealPlanDailySummaryResponse;
import com.eefood.reactionservice.mealplan.dto.response.MealPlanItemIngredientResponse;
import com.eefood.reactionservice.mealplan.dto.response.MealPlanItemResponse;
import com.eefood.reactionservice.mealplan.dto.response.MealPlanResponse;
import com.eefood.reactionservice.mealplan.mapper.MealPlanMapper;
import com.eefood.reactionservice.mealplan.model.MealPlan;
import com.eefood.reactionservice.mealplan.model.MealPlanItem;
import com.eefood.reactionservice.mealplan.model.MealPlanItemIngredient;
import com.eefood.reactionservice.mealplan.repo.MealPlanItemIngredientRepository;
import com.eefood.reactionservice.mealplan.repo.MealPlanItemRepository;
import com.eefood.reactionservice.mealplan.repo.MealPlanRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MealPlanService {

    private final MealPlanRepository mealPlanRepository;
    private final MealPlanItemRepository mealPlanItemRepository;
    private final MealPlanItemIngredientRepository mealPlanItemIngredientRepository;
    private final MealPlanMapper mealPlanMapper;

    @Transactional
    public MealPlanResponse upsertCurrentMealPlan(Long userId, MealPlanUpsertRequest request) {
        validateUpsertRequest(userId, request);

        MealPlan mealPlan = mealPlanRepository.findByUserId(userId)
                .orElseGet(() -> MealPlan.builder().userId(userId).build());

        mealPlan.setGoal(request.getGoal());
        mealPlan.setStartDate(request.getStartDate());
        mealPlan.setEndDate(request.getEndDate());
        mealPlan.setNote(request.getNote());
        mealPlan.setUserHealthNote(request.getUserHealthNote());

        MealPlan savedMealPlan = mealPlanRepository.save(mealPlan);
        return buildMealPlanResponse(savedMealPlan);
    }

    @Transactional
    public MealPlanResponse getCurrentMealPlan(Long userId) {
        if (userId == null) {
            throw ExceptionUtil.badRequest(ErrorMessage.INVALID_REQUEST);
        }

        return mealPlanRepository.findByUserId(userId)
                .map(this::buildMealPlanResponse)
                .orElse(null);
    }

    @Transactional
    public List<MealPlanDailySummaryResponse> getDailySummaries(Long userId) {
        if (userId == null) {
            throw ExceptionUtil.badRequest(ErrorMessage.INVALID_REQUEST);
        }

        return mealPlanRepository.findByUserId(userId)
                .map(mealPlan -> {
                    // Gom tất cả item theo ngày và cộng dồn dinh dưỡng theo servings của từng item.
                    Map<java.time.LocalDate, List<MealPlanItem>> itemsByDate = mealPlanItemRepository
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

    private MealPlanResponse buildMealPlanResponse(MealPlan mealPlan) {
        // Dựng response hoàn chỉnh của meal plan, bao gồm item và ingredients theo từng item.
        MealPlanResponse response = mealPlanMapper.toResponse(mealPlan);

        List<MealPlanItem> items = mealPlanItemRepository
                .findAllByMealPlanIdOrderByPlanDateAscMealSlotAscItemOrderAsc(mealPlan.getId());

        List<Long> itemIds = items.stream()
                .map(MealPlanItem::getId)
                .toList();

        Map<Long, List<MealPlanItemIngredientResponse>> ingredientsByItemId =
                mealPlanItemIngredientRepository.findAllByMealPlanItemIdIn(itemIds).stream()
                        .collect(Collectors.groupingBy(
                                MealPlanItemIngredient::getMealPlanItemId,
                                Collectors.mapping(mealPlanMapper::toResponse, Collectors.toList())
                        ));

        List<MealPlanItemResponse> itemResponses = items.stream()
                .map(item -> {
                    MealPlanItemResponse itemResponse = mealPlanMapper.toResponse(item);
                    itemResponse.setIngredients(ingredientsByItemId.getOrDefault(item.getId(), List.of()));
                    return itemResponse;
                })
                .toList();

        response.setItems(itemResponses);
        return response;
    }

    private void validateUpsertRequest(Long userId, MealPlanUpsertRequest request) {
        if (userId == null || request == null) {
            throw ExceptionUtil.badRequest(ErrorMessage.INVALID_REQUEST);
        }

        if (request.getStartDate() == null || request.getEndDate() == null) {
            throw ExceptionUtil.badRequest(ErrorMessage.INVALID_REQUEST);
        }

        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw ExceptionUtil.badRequest(ErrorMessage.INVALID_REQUEST);
        }

    }

    private MealPlanDailySummaryResponse toDailySummary(java.time.LocalDate planDate, List<MealPlanItem> items) {
        // Ưu tiên actualServings nếu đã có, ngược lại dùng plannedServings để tính tổng theo ngày.
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
}
