package com.eefood.reactionservice.mealplan.service;

import com.eefood.reactionservice.enums.ErrorMessage;
import com.eefood.reactionservice.exception.ExceptionUtil;
import com.eefood.reactionservice.mealplan.dto.request.MealPlanUpsertRequest;
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

import java.time.temporal.ChronoUnit;
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

    private MealPlanResponse buildMealPlanResponse(MealPlan mealPlan) {
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

        long planDays = ChronoUnit.DAYS.between(request.getStartDate(), request.getEndDate()) + 1;
        if (planDays > MAX_PLAN_DAYS) {
            throw ExceptionUtil.badRequest(ErrorMessage.INVALID_REQUEST);
        }
    }
}
