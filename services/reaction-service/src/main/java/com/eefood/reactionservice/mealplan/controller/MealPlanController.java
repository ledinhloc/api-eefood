package com.eefood.reactionservice.mealplan.controller;

import com.eefood.reactionservice.dto.response.ResponseData;
import com.eefood.reactionservice.mealplan.dto.request.MealPlanUpsertRequest;
import com.eefood.reactionservice.mealplan.dto.response.MealPlanResponse;
import com.eefood.reactionservice.mealplan.service.MealPlanService;
import com.eefood.reactionservice.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/meal-plan")
@RequiredArgsConstructor
public class MealPlanController {

    private final MealPlanService mealPlanService;
    private final SecurityUtil securityUtil;

    @GetMapping
    public ResponseData<MealPlanResponse> getCurrentMealPlan() {
        Long userId = securityUtil.getCurrentUserId();
        return new ResponseData<>(
                HttpStatus.OK.value(),
                "Get Meal Plan Success",
                mealPlanService.getCurrentMealPlan(userId)
        );
    }

    @PutMapping
    public ResponseData<MealPlanResponse> upsertMealPlan(@RequestBody MealPlanUpsertRequest request) {
        Long userId = securityUtil.getCurrentUserId();
        return new ResponseData<>(
                HttpStatus.OK.value(),
                "Update Meal Plan Success",
                mealPlanService.upsertCurrentMealPlan(userId, request)
        );
    }
}
