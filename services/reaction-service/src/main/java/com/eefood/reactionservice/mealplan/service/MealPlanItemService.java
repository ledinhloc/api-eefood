package com.eefood.reactionservice.mealplan.service;

import com.eefood.reactionservice.dto.request.MealPlanNutritionIngredientRequest;
import com.eefood.reactionservice.dto.request.ShoppingMealPlanIngredientRequest;
import com.eefood.reactionservice.dto.request.ShoppingMealPlanItemRequest;
import com.eefood.reactionservice.dto.response.ShoppingItemDto;
import com.eefood.reactionservice.dto.response.UserBodyMetricsResponse;
import com.eefood.reactionservice.dto.response.UserResponse;
import com.eefood.reactionservice.enums.ErrorMessage;
import com.eefood.reactionservice.enums.PostStatus;
import com.eefood.reactionservice.exception.ExceptionUtil;
import com.eefood.reactionservice.mealplan.dto.ai.GeneratedMealReplacement;
import com.eefood.reactionservice.mealplan.dto.ai.MealPlanAiCandidate;
import com.eefood.reactionservice.mealplan.dto.request.MealPlanItemUpsertRequest;
import com.eefood.reactionservice.mealplan.dto.request.MealPlanRegenerateItemsRequest;
import com.eefood.reactionservice.mealplan.dto.response.MealPlanItemIngredientResponse;
import com.eefood.reactionservice.mealplan.dto.response.MealPlanItemResponse;
import com.eefood.reactionservice.mealplan.dto.response.MealPlanResponse;
import com.eefood.reactionservice.mealplan.dto.response.NutritionAnalysisResponse;
import com.eefood.reactionservice.mealplan.enums.MealPlanItemSource;
import com.eefood.reactionservice.mealplan.enums.MealPlanItemStatus;
import com.eefood.reactionservice.mealplan.mapper.MealPlanItemMapper;
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
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MealPlanItemService {
    private static final int MAX_REASON_LENGTH = 500;

    private final MealPlanRepository mealPlanRepository;
    private final MealPlanItemRepository mealPlanItemRepository;
    private final PostRepository postRepository;
    private final RecipeClient recipeClient;
    private final MealPlanItemMapper mealPlanItemMapper;
    private final MealPlanService mealPlanService;
    private final MealPlanIngredientService mealPlanIngredientService;
    private final MealPlanCandidateService mealPlanCandidateService;
    private final MealPlanAiPlannerService mealPlanAiPlannerService;
    private final IamClient iamClient;

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

        Long previousRecipeId = item.getRecipeId();
        applyItemRequest(item, request);
        validateDuplicateItem(item);
        MealPlanItem savedItem = mealPlanItemRepository.save(item);

        // Nếu client gửi ingredients thì coi như muốn đồng bộ lại toàn bộ danh sách nguyên liệu.
        if (request.getIngredients() != null) {
            mealPlanIngredientService.replaceIngredients(savedItem.getId(), request.getIngredients());
            List<MealPlanNutritionIngredientRequest> nutritionIngredients = request.getIngredients().stream()
                    .filter(Objects::nonNull)
                    .filter(ingredient -> ingredient.getName() != null && !ingredient.getName().isBlank())
                    .map(ingredient -> MealPlanNutritionIngredientRequest.builder()
                            .name(ingredient.getName())
                            .quantity(ingredient.getQuantity())
                            .unit(ingredient.getUnit())
                            .build())
                    .toList();
            NutritionAnalysisResponse nutrition = recipeClient.calculateMealPlanNutrition(nutritionIngredients).getData();
            if (nutrition != null) {
                savedItem.setCalories(toBigDecimal(nutrition.getTotalCalories()));
                savedItem.setProtein(toBigDecimal(nutrition.getTotalProtein()));
                savedItem.setCarbs(toBigDecimal(nutrition.getTotalCarb()));
                savedItem.setFat(toBigDecimal(nutrition.getTotalFat()));
                savedItem.setFiber(toBigDecimal(nutrition.getTotalFiber()));
                savedItem.setSugar(toBigDecimal(nutrition.getTotalSugar()));
                savedItem.setCalcium(toBigDecimal(nutrition.getTotalCalcium()));
                savedItem.setSodium(toBigDecimal(nutrition.getTotalSodium()));
                savedItem = mealPlanItemRepository.save(savedItem);
            }
        } else if (savedItem.getItemSource() == MealPlanItemSource.RECIPE
                && !Objects.equals(previousRecipeId, savedItem.getRecipeId())) {
            // Tao snapshot nguyen lieu khi them moi hoac doi recipe.
            mealPlanIngredientService.replaceIngredientsFromRecipe(savedItem.getId(), savedItem.getRecipeId());
        }

        return buildItemResponse(savedItem);
    }

    @Transactional
    public List<MealPlanItemResponse> regenerateMealPlanItems(
            Long userId,
            MealPlanRegenerateItemsRequest request
    ) {
        if (userId == null || request == null || request.getItemIds() == null
                || request.getItemIds().isEmpty()
                || request.getItemIds().stream().anyMatch(Objects::isNull)
                || request.getItemIds().stream().distinct().count() != request.getItemIds().size()
                || request.getReason() != null && request.getReason().length() > MAX_REASON_LENGTH) {
            throw ExceptionUtil.badRequest(ErrorMessage.INVALID_REQUEST);
        }

        MealPlan mealPlan = mealPlanRepository.findByUserId(userId)
                .orElseThrow(() -> ExceptionUtil.notFound(ErrorMessage.MEAL_PLAN_NOT_FOUND));
        //lấy các item cần đổi
        List<MealPlanItem> replacedItems = mealPlanItemRepository
                .findAllByIdInAndMealPlanId(request.getItemIds(), mealPlan.getId());
        if (replacedItems.size() != request.getItemIds().size()) {
            throw ExceptionUtil.notFound(ErrorMessage.MEAL_PLAN_ITEM_NOT_FOUND);
        }
        //món có trạng thái done ko được đổi
        if (replacedItems.stream().anyMatch(item -> item.getStatus() == MealPlanItemStatus.DONE)) {
            throw ExceptionUtil.badRequest(ErrorMessage.INVALID_REQUEST);
        }
        //lấy recipe đang sử dụng
        Set<Long> existingRecipeIds = mealPlanItemRepository
                .findAllByMealPlanIdOrderByPlanDateAscMealSlotAscItemOrderAsc(mealPlan.getId())
                .stream()
                .map(MealPlanItem::getRecipeId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        //data user
        UserResponse user = iamClient.getUserById(userId).getData();
        UserBodyMetricsResponse bodyMetrics = iamClient.getUserBodyMetrics(userId).getData();

        //chon candidate
        List<MealPlanAiCandidate> candidates = mealPlanCandidateService.loadReplacementCandidates(
                userId,
                user,
                mealPlan.getGoal(),
                request.getReason(),
                existingRecipeIds,
                replacedItems.stream()
                        .map(MealPlanItem::getMealSlot)
                        .filter(Objects::nonNull)
                        .distinct()
                        .toList(),
                replacedItems.size()
        );
        if (candidates.size() < replacedItems.size()) {
            throw ExceptionUtil.notFound(ErrorMessage.RECIPE_NOT_FOUND);
        }

        //goi ai chon mon
        List<GeneratedMealReplacement> aiReplacements = mealPlanAiPlannerService.generateMealReplacements(
                user,
                bodyMetrics,
                mealPlan.getGoal(),
                request.getReason(),
                replacedItems,
                candidates
        );
        Set<Long> requestedItemIds = new HashSet<>(request.getItemIds());
        Set<Long> candidateRecipeIds = candidates.stream()
                .map(MealPlanAiCandidate::getRecipeId)
                .collect(Collectors.toSet());
        Set<Long> usedRecipeIds = new HashSet<>();
        Map<Long, GeneratedMealReplacement> replacementsByItemId = new HashMap<>();
        //kiểm tra kết quả AI
        for (GeneratedMealReplacement replacement : aiReplacements) {
            if (replacement == null || replacement.getMealPlanItemId() == null || replacement.getCandidate() == null
                    || !requestedItemIds.contains(replacement.getMealPlanItemId())
                    || !candidateRecipeIds.contains(replacement.getCandidate().getRecipeId())
                    || replacementsByItemId.containsKey(replacement.getMealPlanItemId())
                    || !usedRecipeIds.add(replacement.getCandidate().getRecipeId())) {
                continue;
            }
            //nếu có lỗi bỏ qua
            replacementsByItemId.put(replacement.getMealPlanItemId(), replacement);
        }
        //Fallback cho kết quả bị thiếu
        for (MealPlanItem item : replacedItems) {
            if (replacementsByItemId.containsKey(item.getId())) {
                continue;
            }
            MealPlanAiCandidate fallbackCandidate = candidates.stream()
                    .filter(candidate -> usedRecipeIds.add(candidate.getRecipeId()))
                    .findFirst()
                    .orElseThrow(() -> ExceptionUtil.notFound(ErrorMessage.RECIPE_NOT_FOUND));

            //tạo kết quả thay món cho item bị AI bỏ sót
            replacementsByItemId.put(item.getId(), GeneratedMealReplacement.builder()
                    .mealPlanItemId(item.getId())
                    .servings(item.getPlannedServings() == null ? 1 : item.getPlannedServings())
                    .note("Món thay thế được hệ thống đề xuất")
                    .candidate(fallbackCandidate)
                    .build());
        }

        //cập nhật các item
        Map<Long, MealPlanItem> itemsById = replacedItems.stream()
                .collect(Collectors.toMap(MealPlanItem::getId, Function.identity()));
        List<MealPlanItem> updatedItems = new ArrayList<>();
        for (Long itemId : request.getItemIds()) {
            MealPlanItem item = itemsById.get(itemId);
            GeneratedMealReplacement replacement = replacementsByItemId.get(itemId);
            MealPlanAiCandidate candidate = replacement.getCandidate();

            applyRecipeCandidate(item, candidate);
            item.setPlannedServings(replacement.getServings() == null || replacement.getServings() <= 0
                    ? 1
                    : replacement.getServings());
            item.setNote(replacement.getNote());
            updatedItems.add(item);
        }

        //lưu db
        List<MealPlanItem> savedItems = mealPlanItemRepository.saveAll(updatedItems);
        savedItems.forEach(item ->
                mealPlanIngredientService.replaceIngredientsFromRecipe(item.getId(), item.getRecipeId())
        );
        List<MealPlanItemResponse> responses = savedItems.stream()
                .map(mealPlanItemMapper::toScaledResponse)
                .toList();
        mealPlanIngredientService.hydrateIngredients(responses);
        return responses;
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

        mealPlanIngredientService.deleteIngredientsByItemId(item.getId());
        mealPlanItemRepository.delete(item);

        return mealPlanService.getCurrentMealPlan(userId);
    }

    public List<ShoppingItemDto> addMealPlanItemsToShopping(Long userId, List<Long> itemIds) {
        if (userId == null || itemIds == null || itemIds.isEmpty()
                || itemIds.stream().anyMatch(Objects::isNull)
                || itemIds.stream().distinct().count() != itemIds.size()) {
            throw ExceptionUtil.badRequest(ErrorMessage.INVALID_REQUEST);
        }

        MealPlan mealPlan = mealPlanRepository.findByUserId(userId)
                .orElseThrow(() -> ExceptionUtil.notFound(ErrorMessage.MEAL_PLAN_NOT_FOUND));

        List<MealPlanItem> items = mealPlanItemRepository.findAllByIdInAndMealPlanId(itemIds, mealPlan.getId());
        if (items.size() != itemIds.size()) {
            throw ExceptionUtil.notFound(ErrorMessage.MEAL_PLAN_ITEM_NOT_FOUND);
        }

        Map<Long, MealPlanItem> itemsById = items.stream()
                .collect(Collectors.toMap(MealPlanItem::getId, Function.identity()));
        List<ShoppingMealPlanItemRequest> requests = itemIds.stream()
                .map(itemsById::get)
                .map(this::toShoppingMealPlanItemRequest)
                .toList();

        return recipeClient.addMealPlanItemsToShopping(userId, requests).getData();
    }

    // @Transactional
    // public MealPlanItemResponse getMealPlanItemDetail(Long userId, Long itemId) {
    //     if (userId == null || itemId == null) {
    //         throw ExceptionUtil.badRequest(ErrorMessage.INVALID_REQUEST);
    //     }

    //     MealPlan mealPlan = mealPlanRepository.findByUserId(userId)
    //             .orElseThrow(() -> ExceptionUtil.notFound(ErrorMessage.MEAL_PLAN_NOT_FOUND));

    //     MealPlanItem item = mealPlanItemRepository.findByIdAndMealPlanId(itemId, mealPlan.getId())
    //             .orElseThrow(() -> ExceptionUtil.notFound(ErrorMessage.MEAL_PLAN_ITEM_NOT_FOUND));

    //     return buildItemResponse(item);
    // }

    private MealPlanItemResponse buildItemResponse(MealPlanItem item) {
        // Chỉ nạp lại ingredients của riêng item vừa upsert để tránh over-fetch cả meal plan.
        MealPlanItemResponse response = mealPlanItemMapper.toScaledResponse(item);
        response.setIngredients(mealPlanIngredientService.getIngredientResponses(item.getId()));
        return response;
    }

    private ShoppingMealPlanItemRequest toShoppingMealPlanItemRequest(MealPlanItem item) {
        List<MealPlanItemIngredientResponse> ingredients = mealPlanIngredientService.getIngredientResponses(item.getId());
        if (ingredients.isEmpty()) {
            throw ExceptionUtil.badRequest(ErrorMessage.INVALID_REQUEST);
        }

        return ShoppingMealPlanItemRequest.builder()
                .recipeId(item.getRecipeId())
                .recipeTitle(resolveMealTitle(item))
                .servings(item.getActualServings() != null
                        ? item.getActualServings()
                        : item.getPlannedServings() == null ? 1 : item.getPlannedServings())
                .ingredients(ingredients.stream()
                        .map(ingredient -> ShoppingMealPlanIngredientRequest.builder()
                                .name(ingredient.getName())
                                .quantity(ingredient.getQuantity())
                                .unit(ingredient.getUnit())
                                .build())
                        .toList())
                .build();
    }

    private String resolveMealTitle(MealPlanItem item) {
        if (item.getRecipeTitle() != null && !item.getRecipeTitle().isBlank()) {
            return item.getRecipeTitle();
        }
        return item.getCustomMealName();
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

        applyRecipeCandidate(item, MealPlanAiCandidate.builder()
                .recipeId(post.getRecipeId())
                .postId(post.getId())
                .title(post.getTitle())
                .imageUrl(post.getImageUrl())
                .nutrition(nutrition)
                .build());
    }

    private void applyRecipeCandidate(MealPlanItem item, MealPlanAiCandidate candidate) {
        item.setItemSource(MealPlanItemSource.RECIPE);
        item.setRecipeId(candidate.getRecipeId());
        item.setPostId(candidate.getPostId());
        item.setCustomMealName(null);
        item.setRecipeTitle(candidate.getTitle());
        item.setImageUrl(candidate.getImageUrl());
        item.setCalories(toBigDecimal(candidate.getNutrition().getTotalCalories()));
        item.setProtein(toBigDecimal(candidate.getNutrition().getTotalProtein()));
        item.setCarbs(toBigDecimal(candidate.getNutrition().getTotalCarb()));
        item.setFat(toBigDecimal(candidate.getNutrition().getTotalFat()));
        item.setFiber(toBigDecimal(candidate.getNutrition().getTotalFiber()));
        item.setSugar(toBigDecimal(candidate.getNutrition().getTotalSugar()));
        item.setCalcium(toBigDecimal(candidate.getNutrition().getTotalCalcium()));
        item.setSodium(toBigDecimal(candidate.getNutrition().getTotalSodium()));
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

    private void validateDuplicateItem(MealPlanItem item) {
        if (item.getItemSource() != MealPlanItemSource.RECIPE || item.getRecipeId() == null) {
            return;
        }

        mealPlanItemRepository
                .findFirstByMealPlanIdAndPlanDateAndMealSlotAndRecipeId(
                        item.getMealPlanId(),
                        item.getPlanDate(),
                        item.getMealSlot(),
                        item.getRecipeId()
                )
                .filter(existing -> !existing.getId().equals(item.getId()))
                .ifPresent(existing -> {
                    throw ExceptionUtil.conflict(ErrorMessage.MEAL_PLAN_ITEM_DUPLICATE);
                });
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
