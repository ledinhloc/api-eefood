package com.eefood.reactionservice.mealplan.service;

import com.eefood.reactionservice.enums.ErrorMessage;
import com.eefood.reactionservice.exception.ExceptionUtil;
import com.eefood.reactionservice.mealplan.dto.request.MealPlanItemIngredientUpsertRequest;
import com.eefood.reactionservice.mealplan.dto.request.MealPlanItemUpsertRequest;
import com.eefood.reactionservice.mealplan.dto.request.MealPlanUpsertRequest;
import com.eefood.reactionservice.mealplan.dto.response.MealPlanItemIngredientResponse;
import com.eefood.reactionservice.mealplan.dto.response.MealPlanItemResponse;
import com.eefood.reactionservice.mealplan.dto.response.MealPlanResponse;
import com.eefood.reactionservice.mealplan.dto.response.NutritionAnalysisResponse;
import com.eefood.reactionservice.mealplan.mapper.MealPlanMapper;
import com.eefood.reactionservice.mealplan.model.MealPlan;
import com.eefood.reactionservice.mealplan.model.MealPlanItem;
import com.eefood.reactionservice.mealplan.model.MealPlanItemIngredient;
import com.eefood.reactionservice.mealplan.enums.MealPlanItemSource;
import com.eefood.reactionservice.mealplan.enums.MealPlanItemStatus;
import com.eefood.reactionservice.mealplan.repo.MealPlanItemIngredientRepository;
import com.eefood.reactionservice.mealplan.repo.MealPlanItemRepository;
import com.eefood.reactionservice.mealplan.repo.MealPlanRepository;
import com.eefood.reactionservice.model.Post;
import com.eefood.reactionservice.repository.httpclient.RecipeClient;
import com.eefood.reactionservice.repository.post.PostRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import org.springframework.stereotype.Service;

import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MealPlanService {

    private static final long MAX_PLAN_DAYS = 14L;

    private final MealPlanRepository mealPlanRepository;
    private final MealPlanItemRepository mealPlanItemRepository;
    private final MealPlanItemIngredientRepository mealPlanItemIngredientRepository;
    private final MealPlanMapper mealPlanMapper;
    private final PostRepository postRepository;
    private final RecipeClient recipeClient;

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
    public MealPlanResponse upsertMealPlanItem(Long userId, MealPlanItemUpsertRequest request) {
        // Upsert item trong meal plan của user, rồi trả lại toàn bộ plan đã cập nhật.
        MealPlan mealPlan = mealPlanRepository.findByUserId(userId)
                .orElseThrow(() -> ExceptionUtil.notFound(ErrorMessage.MEAL_PLAN_NOT_FOUND));

        MealPlanItem item = request.getId() == null
                ? MealPlanItem.builder().mealPlanId(mealPlan.getId()).build()
                : mealPlanItemRepository.findByIdAndMealPlanId(request.getId(), mealPlan.getId())
                .orElseThrow(() -> ExceptionUtil.notFound(ErrorMessage.MEAL_PLAN_ITEM_NOT_FOUND));

        validateItemUpsertRequest(userId, request, item);
        validatePlanDateWithinMealPlan(mealPlan, request.getPlanDate() != null ? request.getPlanDate() : item.getPlanDate());

        applyItemRequest(item, request);
        MealPlanItem savedItem = mealPlanItemRepository.save(item);

        if (request.getIngredients() != null) {
            replaceIngredients(savedItem.getId(), request.getIngredients());
        }

        return buildMealPlanResponse(mealPlan);
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

    private void applyItemRequest(MealPlanItem item, MealPlanItemUpsertRequest request) {
        // Áp dụng partial update cho các field chung trước khi rẽ sang RECIPE hoặc CUSTOM.
        if (request.getPlanDate() != null) {
            item.setPlanDate(request.getPlanDate());
        }
        if (request.getMealSlot() != null) {
            item.setMealSlot(request.getMealSlot());
        }
        item.setItemOrder(request.getItemOrder() == null ? defaultValue(item.getItemOrder(), 1) : request.getItemOrder());
        if (request.getPlannedServings() != null || item.getId() == null) {
            item.setPlannedServings(request.getPlannedServings());
        }
        if (request.getActualServings() != null || item.getId() == null) {
            item.setActualServings(request.getActualServings());
        }
        item.setStatus(request.getStatus() == null ? defaultValue(item.getStatus(), MealPlanItemStatus.PLANNED) : request.getStatus());
        if (request.getNote() != null || item.getId() == null) {
            item.setNote(request.getNote());
        }

        MealPlanItemSource itemSource = request.getItemSource() != null ? request.getItemSource() : item.getItemSource();
        if (itemSource == MealPlanItemSource.CUSTOM) {
            applyCustomItem(item, request);
            return;
        }

        applyRecipeItem(item, request);
    }

    private void applyCustomItem(MealPlanItem item, MealPlanItemUpsertRequest request) {
        // Custom item không gắn recipe/post và xoá toàn bộ snapshot nutrition có sẵn.
        item.setItemSource(MealPlanItemSource.CUSTOM);
        item.setRecipeId(null);
        item.setPostId(null);
        String customMealName = request.getCustomMealName() != null ? request.getCustomMealName() : item.getCustomMealName();
        item.setCustomMealName(customMealName);
        item.setRecipeTitleSnapshot(customMealName);
        item.setImageUrlSnapshot(null);
        item.setRecipeServingsSnapshot(null);
        item.setCaloriesPerServingSnapshot(null);
        item.setProteinPerServingSnapshot(null);
        item.setCarbsPerServingSnapshot(null);
        item.setFatPerServingSnapshot(null);
        item.setFiberPerServingSnapshot(null);
        item.setSugarPerServingSnapshot(null);
        item.setCalciumPerServingSnapshot(null);
        item.setSodiumPerServingSnapshot(null);
    }

    private void applyRecipeItem(MealPlanItem item, MealPlanItemUpsertRequest request) {
        // Nếu chỉ sửa field phụ của item recipe hiện có thì giữ nguyên recipe snapshot cũ.
        if (request.getItemSource() == null && request.getPostId() == null && request.getRecipeId() == null && item.getRecipeId() != null) {
            return;
        }

        // Khi đổi món recipe, nạp lại post + nutrition để cập nhật snapshot theo recipe mới.
        Post post = resolveRecipePost(request);
        NutritionAnalysisResponse nutrition = recipeClient.getNutritionByRecipeId(post.getRecipeId(), false).getData();
        if (nutrition == null) {
            throw ExceptionUtil.notFound(ErrorMessage.RECIPE_NOT_FOUND);
        }

        item.setItemSource(MealPlanItemSource.RECIPE);
        item.setRecipeId(post.getRecipeId());
        item.setPostId(post.getId());
        item.setCustomMealName(null);
        item.setRecipeTitleSnapshot(post.getTitle());
        item.setImageUrlSnapshot(post.getImageUrl());
        item.setRecipeServingsSnapshot(1);
        item.setCaloriesPerServingSnapshot(toBigDecimal(nutrition.getTotalCalories()));
        item.setProteinPerServingSnapshot(toBigDecimal(nutrition.getTotalProtein()));
        item.setCarbsPerServingSnapshot(toBigDecimal(nutrition.getTotalCarb()));
        item.setFatPerServingSnapshot(toBigDecimal(nutrition.getTotalFat()));
        item.setFiberPerServingSnapshot(toBigDecimal(nutrition.getTotalFiber()));
        item.setSugarPerServingSnapshot(toBigDecimal(nutrition.getTotalSugar()));
        item.setCalciumPerServingSnapshot(toBigDecimal(nutrition.getTotalCalcium()));
        item.setSodiumPerServingSnapshot(toBigDecimal(nutrition.getTotalSodium()));
    }

    private Post resolveRecipePost(MealPlanItemUpsertRequest request) {
        // Cho phép client truyền postId hoặc recipeId, ưu tiên postId nếu có.
        Post post = request.getPostId() != null
                ? postRepository.findByIdAndIsDeletedFalse(request.getPostId())
                : request.getRecipeId() != null
                ? postRepository.findByRecipeIdAndIsDeletedFalse(request.getRecipeId())
                : null;
        if (post == null || post.getRecipeId() == null) {
            throw ExceptionUtil.notFound(ErrorMessage.RECIPE_NOT_FOUND);
        }
        return post;
    }

    private void replaceIngredients(Long mealPlanItemId, List<MealPlanItemIngredientUpsertRequest> ingredients) {
        // Danh sách ingredients được đồng bộ theo kiểu replace-all để FE dễ quản lý state.
        mealPlanItemIngredientRepository.deleteAllByMealPlanItemId(mealPlanItemId);

        List<MealPlanItemIngredient> entities = ingredients.stream()
                .filter(Objects::nonNull)
                .filter(ingredient -> ingredient.getName() != null && !ingredient.getName().isBlank())
                .map(ingredient -> MealPlanItemIngredient.builder()
                        .mealPlanItemId(mealPlanItemId)
                        .name(ingredient.getName().trim())
                        .quantity(ingredient.getQuantity())
                        .unit(ingredient.getUnit())
                        .note(ingredient.getNote())
                        .build())
                .collect(Collectors.toList());

        if (!entities.isEmpty()) {
            mealPlanItemIngredientRepository.saveAll(entities);
        }
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

    private void validateItemUpsertRequest(Long userId, MealPlanItemUpsertRequest request, MealPlanItem existingItem) {
        // Tạo mới bắt buộc đủ field chính, còn update thì cho phép gửi partial.
        if (userId == null || request == null) {
            throw ExceptionUtil.badRequest(ErrorMessage.INVALID_REQUEST);
        }

        boolean creating = existingItem.getId() == null;
        if (creating && (request.getPlanDate() == null || request.getMealSlot() == null || request.getItemSource() == null)) {
            throw ExceptionUtil.badRequest(ErrorMessage.INVALID_REQUEST);
        }

        MealPlanItemSource itemSource = request.getItemSource() != null ? request.getItemSource() : existingItem.getItemSource();
        if (itemSource == MealPlanItemSource.CUSTOM
                && isBlank(request.getCustomMealName())
                && isBlank(existingItem.getCustomMealName())) {
            throw ExceptionUtil.badRequest(ErrorMessage.INVALID_REQUEST);
        }

        if (itemSource == MealPlanItemSource.RECIPE
                && request.getPostId() == null && request.getRecipeId() == null
                && existingItem.getRecipeId() == null) {
            throw ExceptionUtil.badRequest(ErrorMessage.INVALID_REQUEST);
        }
    }

    private void validatePlanDateWithinMealPlan(MealPlan mealPlan, java.time.LocalDate planDate) {
        // Chỉ cho phép sửa item trong khoảng ngày mà meal plan hiện tại đang quản lý.
        if (planDate == null || mealPlan.getStartDate() == null || mealPlan.getEndDate() == null) {
            throw ExceptionUtil.badRequest(ErrorMessage.INVALID_REQUEST);
        }

        if (planDate.isBefore(mealPlan.getStartDate()) || planDate.isAfter(mealPlan.getEndDate())) {
            throw ExceptionUtil.badRequest(ErrorMessage.INVALID_REQUEST);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private <T> T defaultValue(T value, T fallback) {
        return value == null ? fallback : value;
    }

    private BigDecimal toBigDecimal(Double value) {
        return value == null ? null : BigDecimal.valueOf(value);
    }
}
