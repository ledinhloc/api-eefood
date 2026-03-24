package com.eefood.reactionservice.mealplan.service;

import com.eefood.reactionservice.dto.request.MealPlanUpsertRequest;
import com.eefood.reactionservice.dto.response.MealPlanResponse;
import com.eefood.reactionservice.enums.ErrorMessage;
import com.eefood.reactionservice.exception.ExceptionUtil;
import com.eefood.reactionservice.mapper.MealPlanMapper;
import com.eefood.reactionservice.mealplan.model.MealPlan;
import com.eefood.reactionservice.mealplan.model.MealPlanItem;
import com.eefood.reactionservice.mealplan.model.MealPlanItemIngredient;
import com.eefood.reactionservice.mealplan.repo.MealPlanItemIngredientRepository;
import com.eefood.reactionservice.mealplan.repo.MealPlanItemRepository;
import com.eefood.reactionservice.mealplan.repo.MealPlanRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MealPlanService {

    private static final long MAX_PLAN_DAYS = 14L;

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

        return mealPlanMapper.toResponse(loadMealPlanDetails(savedMealPlan));
    }

    @Transactional
    public MealPlanResponse getCurrentMealPlan(Long userId) {
        if (userId == null) {
            throw ExceptionUtil.badRequest(ErrorMessage.INVALID_REQUEST);
        }

        return mealPlanRepository.findByUserId(userId)
                .map(this::loadMealPlanDetails)
                .map(mealPlanMapper::toResponse)
                .orElse(null);
    }

    private MealPlan loadMealPlanDetails(MealPlan mealPlan) {
        List<MealPlanItem> items = mealPlanItemRepository
                .findAllByMealPlanIdOrderByPlanDateAscMealSlotAscItemOrderAsc(mealPlan.getId());

        List<Long> itemIds = items.stream()
                .map(MealPlanItem::getId)
                .toList();

        Map<Long, List<MealPlanItemIngredient>> ingredientsByItemId = itemIds.isEmpty()
                ? Collections.emptyMap()
                : mealPlanItemIngredientRepository.findAllByMealPlanItemIdIn(itemIds).stream()
                .collect(Collectors.groupingBy(ingredient -> ingredient.getMealPlanItem().getId()));

        items.forEach(item -> item.setIngredients(
                ingredientsByItemId.getOrDefault(item.getId(), List.of())
        ));

        mealPlan.setItems(items);
        return mealPlan;
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

        long planDays = ChronoUnit.DAYS.between(request.getStartDate(), request.getEndDate()) + 1;
        if (planDays > MAX_PLAN_DAYS) {
            throw ExceptionUtil.badRequest(ErrorMessage.INVALID_REQUEST);
        }
    }
}
