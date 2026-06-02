package com.eefood.reactionservice.mealplan.controller;

import com.eefood.reactionservice.dto.response.ResponseData;
import com.eefood.reactionservice.mealplan.dto.request.MealPlanGenerateRequest;
import com.eefood.reactionservice.mealplan.dto.request.MealPlanItemUpsertRequest;
import com.eefood.reactionservice.mealplan.dto.request.MealPlanUpsertRequest;
import com.eefood.reactionservice.mealplan.dto.response.MealPlanDailySummaryResponse;
import com.eefood.reactionservice.mealplan.dto.response.MealPlanItemResponse;
import com.eefood.reactionservice.mealplan.dto.response.MealPlanResponse;
import com.eefood.reactionservice.mealplan.service.MealPlanItemService;
import com.eefood.reactionservice.mealplan.service.MealPlanService;
import com.eefood.reactionservice.util.SecurityUtil;
import lombok.RequiredArgsConstructor;

import java.time.LocalDate;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/meal-plan")
@RequiredArgsConstructor
public class MealPlanController {

    private final MealPlanService mealPlanService;
    private final MealPlanItemService mealPlanItemService;
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

    @GetMapping("/daily-summary")
    public ResponseData<List<MealPlanDailySummaryResponse>> getDailySummary() {
        Long userId = securityUtil.getCurrentUserId();
        return new ResponseData<>(
                HttpStatus.OK.value(),
                "Get Daily Summary Success",
                mealPlanService.getDailySummaries(userId)
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

    @PostMapping("/generate")
    public ResponseData<MealPlanResponse> generateMealPlan(@RequestBody MealPlanGenerateRequest request) {
        Long userId = securityUtil.getCurrentUserId();
        return new ResponseData<>(
                HttpStatus.OK.value(),
                "Generate Meal Plan Success",
                mealPlanService.generateInitialMealPlan(userId, request)
        );
    }

    @PostMapping("/continue")
    public ResponseData<MealPlanResponse> continueMealPlan(
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) Integer days
    ) {
        Long userId = securityUtil.getCurrentUserId();
        return new ResponseData<>(
                HttpStatus.OK.value(),
                "Continue Meal Plan Success",
                mealPlanService.continueMealPlan(userId, startDate, days)
        );
    }

    @PutMapping("/items")
    public ResponseData<MealPlanItemResponse> upsertMealPlanItem(@RequestBody MealPlanItemUpsertRequest request) {
        // id = null thì thêm mới; id != null thì cập nhật item hiện có.
        Long userId = securityUtil.getCurrentUserId();
        return new ResponseData<>(
                HttpStatus.OK.value(),
                "Update Meal Plan Item Success",
                mealPlanItemService.upsertMealPlanItem(userId, request)
        );
    }

    @GetMapping("/items")
    public ResponseData<List<MealPlanItemResponse>> getMealPlanItemsByDate(@RequestParam LocalDate date) {
        Long userId = securityUtil.getCurrentUserId();
        return new ResponseData<>(
                HttpStatus.OK.value(),
                "Get Meal Plan Items By Date Success",
                mealPlanService.getItemsByDate(userId, date)
        );
    }

    // @GetMapping("/items/{id}")
    // public ResponseData<MealPlanItemResponse> getMealPlanItemDetail(@PathVariable Long id) {
    //     Long userId = securityUtil.getCurrentUserId();
    //     return new ResponseData<>(
    //             HttpStatus.OK.value(),
    //             "Get Meal Plan Item Detail Success",
    //             mealPlanItemService.getMealPlanItemDetail(userId, id)
    //     );
    // }

    @DeleteMapping("/items/{id}")
    public ResponseData<MealPlanResponse> deleteMealPlanItem(@PathVariable Long id) {
        // Xóa hẳn một item khỏi meal plan hiện tại của user.
        Long userId = securityUtil.getCurrentUserId();
        return new ResponseData<>(
                HttpStatus.OK.value(),
                "Delete Meal Plan Item Success",
                mealPlanItemService.deleteMealPlanItem(userId, id)
        );
    }
}
