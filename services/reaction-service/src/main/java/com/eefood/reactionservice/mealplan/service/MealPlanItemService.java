package com.eefood.reactionservice.mealplan.service;

import com.eefood.reactionservice.enums.ErrorMessage;
import com.eefood.reactionservice.enums.PostStatus;
import com.eefood.reactionservice.exception.ExceptionUtil;
import com.eefood.reactionservice.mealplan.dto.request.MealPlanItemIngredientUpsertRequest;
import com.eefood.reactionservice.mealplan.dto.request.MealPlanItemUpsertRequest;
import com.eefood.reactionservice.mealplan.dto.response.MealPlanItemResponse;
import com.eefood.reactionservice.mealplan.dto.response.MealPlanResponse;
import com.eefood.reactionservice.mealplan.dto.response.NutritionAnalysisResponse;
import com.eefood.reactionservice.mealplan.enums.MealPlanItemSource;
import com.eefood.reactionservice.mealplan.enums.MealPlanItemStatus;
import com.eefood.reactionservice.mealplan.mapper.MealPlanMapper;
import com.eefood.reactionservice.mealplan.model.MealPlan;
import com.eefood.reactionservice.mealplan.model.MealPlanItem;
import com.eefood.reactionservice.mealplan.model.MealPlanItemIngredient;
import com.eefood.reactionservice.mealplan.repo.MealPlanItemIngredientRepository;
import com.eefood.reactionservice.mealplan.repo.MealPlanItemRepository;
import com.eefood.reactionservice.mealplan.repo.MealPlanRepository;
import com.eefood.reactionservice.model.Post;
import com.eefood.reactionservice.repository.httpclient.RecipeClient;
import com.eefood.reactionservice.repository.post.PostRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MealPlanItemService {

    private final MealPlanRepository mealPlanRepository;
    private final MealPlanItemRepository mealPlanItemRepository;
    private final MealPlanItemIngredientRepository mealPlanItemIngredientRepository;
    private final PostRepository postRepository;
    private final RecipeClient recipeClient;
    private final MealPlanMapper mealPlanMapper;
    private final MealPlanService mealPlanService;

    @Transactional
    public MealPlanItemResponse upsertMealPlanItem(Long userId, MealPlanItemUpsertRequest request) {
        // Upsert item trong meal plan hiện tại của user, rồi chỉ trả lại item vừa thay đổi.
        MealPlan mealPlan = mealPlanRepository.findByUserId(userId)
                .orElseThrow(() -> ExceptionUtil.notFound(ErrorMessage.MEAL_PLAN_NOT_FOUND));

        // Có id thì sửa item cũ, không có id thì tạo item mới trong meal plan hiện tại.
        MealPlanItem item = request.getId() == null
                ? MealPlanItem.builder().mealPlanId(mealPlan.getId()).build()
                : mealPlanItemRepository.findByIdAndMealPlanId(request.getId(), mealPlan.getId())
                .orElseThrow(() -> ExceptionUtil.notFound(ErrorMessage.MEAL_PLAN_ITEM_NOT_FOUND));

        validateItemUpsertRequest(userId, request, item);
        validatePlanDateWithinMealPlan(mealPlan, request.getPlanDate() != null ? request.getPlanDate() : item.getPlanDate());

        applyItemRequest(item, request);
        MealPlanItem savedItem = mealPlanItemRepository.save(item);

        // Nếu client gửi ingredients thì coi như muốn đồng bộ lại toàn bộ danh sách nguyên liệu.
        if (request.getIngredients() != null) {
            replaceIngredients(savedItem.getId(), request.getIngredients());
        }

        return buildItemResponse(savedItem);
    }

    @Transactional
    public MealPlanResponse deleteMealPlanItem(Long userId, Long itemId) {
        // Xóa hẳn item khỏi meal plan hiện tại của user và dọn luôn custom ingredients liên quan.
        if (userId == null || itemId == null) {
            throw ExceptionUtil.badRequest(ErrorMessage.INVALID_REQUEST);
        }

        MealPlan mealPlan = mealPlanRepository.findByUserId(userId)
                .orElseThrow(() -> ExceptionUtil.notFound(ErrorMessage.MEAL_PLAN_NOT_FOUND));

        MealPlanItem item = mealPlanItemRepository.findByIdAndMealPlanId(itemId, mealPlan.getId())
                .orElseThrow(() -> ExceptionUtil.notFound(ErrorMessage.MEAL_PLAN_ITEM_NOT_FOUND));

        mealPlanItemIngredientRepository.deleteAllByMealPlanItemId(item.getId());
        mealPlanItemRepository.delete(item);

        return mealPlanService.getCurrentMealPlan(userId);
    }

    @Transactional
    public MealPlanItemResponse getMealPlanItemDetail(Long userId, Long itemId) {
        if (userId == null || itemId == null) {
            throw ExceptionUtil.badRequest(ErrorMessage.INVALID_REQUEST);
        }

        MealPlan mealPlan = mealPlanRepository.findByUserId(userId)
                .orElseThrow(() -> ExceptionUtil.notFound(ErrorMessage.MEAL_PLAN_NOT_FOUND));

        MealPlanItem item = mealPlanItemRepository.findByIdAndMealPlanId(itemId, mealPlan.getId())
                .orElseThrow(() -> ExceptionUtil.notFound(ErrorMessage.MEAL_PLAN_ITEM_NOT_FOUND));

        return buildItemResponse(item);
    }

    private MealPlanItemResponse buildItemResponse(MealPlanItem item) {
        // Chỉ nạp lại ingredients của riêng item vừa upsert để tránh over-fetch cả meal plan.
        MealPlanItemResponse response = mealPlanMapper.toResponse(item);
        BigDecimal multiplier = BigDecimal.valueOf(resolveServings(item));

        response.setCalories(scale(item.getCalories(), multiplier));
        response.setProtein(scale(item.getProtein(), multiplier));
        response.setCarbs(scale(item.getCarbs(), multiplier));
        response.setFat(scale(item.getFat(), multiplier));
        response.setFiber(scale(item.getFiber(), multiplier));
        response.setSugar(scale(item.getSugar(), multiplier));
        response.setCalcium(scale(item.getCalcium(), multiplier));
        response.setSodium(scale(item.getSodium(), multiplier));
        response.setIngredients(
                mealPlanItemIngredientRepository.findAllByMealPlanItemIdIn(List.of(item.getId())).stream()
                        .map(mealPlanMapper::toResponse)
                        .toList()
        );
        return response;
    }

    private void applyItemRequest(MealPlanItem item, MealPlanItemUpsertRequest request) {
        // Áp dụng partial update cho các field chung trước khi rẽ nhánh RECIPE hoặc CUSTOM.
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
        // Custom item không dùng recipe/post, nên dọn toàn bộ snapshot liên quan đến recipe cũ.
        item.setItemSource(MealPlanItemSource.CUSTOM);
        item.setRecipeId(null);
        item.setPostId(null);

        String customMealName = request.getCustomMealName() != null ? request.getCustomMealName() : item.getCustomMealName();
        item.setCustomMealName(customMealName);
        item.setRecipeTitle(customMealName);
        item.setImageUrl(null);
        item.setCalories(null);
        item.setProtein(null);
        item.setCarbs(null);
        item.setFat(null);
        item.setFiber(null);
        item.setSugar(null);
        item.setCalcium(null);
        item.setSodium(null);
    }

    private void applyRecipeItem(MealPlanItem item, MealPlanItemUpsertRequest request) {
        // Nếu không có recipe mới trong request thì giữ nguyên snapshot recipe hiện tại.
        if (request.getItemSource() == null && request.getPostId() == null && request.getRecipeId() == null && item.getRecipeId() != null) {
            return;
        }

        // Khi đổi recipe, nạp lại post + nutrition để snapshot luôn khớp với món mới được chọn.
        Post post = resolveRecipePost(request);
        NutritionAnalysisResponse nutrition = recipeClient.getNutritionByRecipeId(post.getRecipeId(), false).getData();
        if (nutrition == null) {
            throw ExceptionUtil.notFound(ErrorMessage.RECIPE_NOT_FOUND);
        }

        item.setItemSource(MealPlanItemSource.RECIPE);
        item.setRecipeId(post.getRecipeId());
        item.setPostId(post.getId());
        item.setCustomMealName(null);
        item.setRecipeTitle(post.getTitle());
        item.setImageUrl(post.getImageUrl());
        item.setCalories(toBigDecimal(nutrition.getTotalCalories()));
        item.setProtein(toBigDecimal(nutrition.getTotalProtein()));
        item.setCarbs(toBigDecimal(nutrition.getTotalCarb()));
        item.setFat(toBigDecimal(nutrition.getTotalFat()));
        item.setFiber(toBigDecimal(nutrition.getTotalFiber()));
        item.setSugar(toBigDecimal(nutrition.getTotalSugar()));
        item.setCalcium(toBigDecimal(nutrition.getTotalCalcium()));
        item.setSodium(toBigDecimal(nutrition.getTotalSodium()));
    }

    private Post resolveRecipePost(MealPlanItemUpsertRequest request) {
        // Chỉ cho phép gắn post đã APPROVED để đồng nhất rule với flow generate meal plan.
        Post post = request.getPostId() != null
                ? postRepository.findByIdAndStatusAndIsDeletedFalse(request.getPostId(), PostStatus.APPROVED)
                : request.getRecipeId() != null
                ? postRepository.findByRecipeIdAndStatusAndIsDeletedFalse(request.getRecipeId(), PostStatus.APPROVED)
                : null;
        if (post == null || post.getRecipeId() == null) {
            throw ExceptionUtil.notFound(ErrorMessage.RECIPE_NOT_FOUND);
        }
        return post;
    }

    private void replaceIngredients(Long mealPlanItemId, List<MealPlanItemIngredientUpsertRequest> ingredients) {
        // Replace-all giúp FE chỉ cần gửi trạng thái ingredients cuối cùng, không phải gửi diff add/remove.
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

    private void validateItemUpsertRequest(Long userId, MealPlanItemUpsertRequest request, MealPlanItem existingItem) {
        // Create mới bắt buộc đủ field chính, còn update thì cho phép partial update.
        if (userId == null || request == null) {
            throw ExceptionUtil.badRequest(ErrorMessage.INVALID_REQUEST);
        }

        // Chặn dữ liệu số không hợp lệ ngay ở API write để tránh làm sai summary về sau.
        if (request.getItemOrder() != null && request.getItemOrder() <= 0) {
            throw ExceptionUtil.badRequest(ErrorMessage.INVALID_REQUEST);
        }
        if (request.getPlannedServings() != null && request.getPlannedServings() <= 0) {
            throw ExceptionUtil.badRequest(ErrorMessage.INVALID_REQUEST);
        }
        if (request.getActualServings() != null && request.getActualServings() <= 0) {
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

    private void validatePlanDateWithinMealPlan(MealPlan mealPlan, LocalDate planDate) {
        // Item chỉ hợp lệ khi nằm trong khoảng ngày mà meal plan hiện tại đang quản lý.
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

    private int resolveServings(MealPlanItem item) {
        Integer servings = item.getActualServings() != null ? item.getActualServings() : item.getPlannedServings();
        return servings == null || servings <= 0 ? 1 : servings;
    }

    private BigDecimal scale(BigDecimal value, BigDecimal multiplier) {
        return value == null ? BigDecimal.ZERO : value.multiply(multiplier);
    }

    private BigDecimal toBigDecimal(Double value) {
        return value == null ? null : BigDecimal.valueOf(value);
    }

}
