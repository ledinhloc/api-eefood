package com.eefood.recipeservice.service.nutrition;

import com.eefood.recipeservice.dto.request.MealPlanNutritionIngredientRequest;
import com.eefood.recipeservice.dto.response.AINutritionResult;
import com.eefood.recipeservice.dto.response.IngredientNutritionDetail;
import com.eefood.recipeservice.dto.response.NutritionAnalysisResponse;
import com.eefood.recipeservice.enums.ErrorMessage;
import com.eefood.recipeservice.enums.HealthLevel;
import com.eefood.recipeservice.exception.ExceptionUtil;
import com.eefood.recipeservice.mapper.NutritionMapper;
import com.eefood.recipeservice.model.Ingredient;
import com.eefood.recipeservice.model.IngredientNutrition;
import com.eefood.recipeservice.model.Recipe;
import com.eefood.recipeservice.model.RecipeIngredient;
import com.eefood.recipeservice.model.RecipeIngredientNutrition;
import com.eefood.recipeservice.model.RecipeNutrition;
import com.eefood.recipeservice.model.RecipeNutritionAnalysis;
import com.eefood.recipeservice.repository.IngredientRepository;
import com.eefood.recipeservice.repository.RecipeRepository;
import com.eefood.recipeservice.repository.httpclient.ReactionClient;
import com.eefood.recipeservice.repository.nutrition.IngredientNutritionRepository;
import com.eefood.recipeservice.repository.nutrition.RecipeNutritionAnalysisRepository;
import com.eefood.recipeservice.repository.nutrition.RecipeNutritionRepository;
import com.eefood.recipeservice.util.NutritionSseUtils;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class NutritionAnalysisService {
    private final RecipeRepository recipeRepository;
    private final IngredientRepository ingredientRepository;
    private final RecipeNutritionRepository recipeNutritionRepository;
    private final RecipeNutritionAnalysisRepository recipeNutritionAnalysisRepository;
    private final IngredientNutritionRepository ingredientNutritionRepository;
    private final AIService aiService;
    private final NutritionMapper nutritionMapper;
    private final NutritionCalculator calculator;
    private final NutritionPromptBuilder promptBuilder;
    private final RedisTemplate<String, Object> redisTemplate;
    private final Executor nutritionAiExecutor;
    private static final String CACHE_PREFIX = "nutrition:recipe:";
    private static final Duration CACHE_TTL = Duration.ofHours(24);
    private final NutritionSseUtils nutritionSseUtils;
    private final ReactionClient reactionClient;
    private final RecipeIngredientNutritionResolver recipeIngredientNutritionResolver;

    // Tra ve du lieu dinh duong dang JSON full có summary và recommendation de service khac goi truc tiep.
    @Transactional
    public NutritionAnalysisResponse getNutritionByRecipeIdFull(Long recipeId, boolean forceRefresh) {
        String cacheKey = CACHE_PREFIX + recipeId;

        if (!forceRefresh) {
            NutritionAnalysisResponse cached =
                    (NutritionAnalysisResponse) redisTemplate.opsForValue().get(cacheKey);
            if (cached != null) {
                log.info("[Nutrition] Cache hit for recipeId={}", recipeId);
                return cached;
            }
        }

        Recipe recipe = recipeRepository.findByIdWithIngredients(recipeId)
                .orElseThrow(() -> ExceptionUtil.notFound(ErrorMessage.RECIPE_NOT_FOUND));

        if (recipe.getIngredients() == null || recipe.getIngredients().isEmpty()) {
            throw ExceptionUtil.badRequest(ErrorMessage.RECIPE_HAS_NO_INGREDIENTS);
        }

        List<RecipeIngredient> ingredients = new ArrayList<>(recipe.getIngredients());
        List<RecipeIngredientNutrition> rinList =
                recipeIngredientNutritionResolver.resolveIngredientNutritions(recipe, ingredients);

        RecipeNutrition nutrition = calculateAndSaveTotalNutrition(recipe, rinList);
        List<IngredientNutritionDetail> details = rinList.stream()
                .map(nutritionMapper::toDetail)
                .toList();

        RecipeNutritionAnalysis analysis;
        try {
            analysis = CompletableFuture
                    .supplyAsync(() -> aiAnalyzeAndSave(recipe, nutrition), nutritionAiExecutor)
                    .get(15, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("[Nutrition SSE] AI analysis timed out, using fallback");
            analysis = buildFallbackAnalysis(recipe, nutrition);
        }

        NutritionAnalysisResponse fullResponse = nutritionMapper.toResponse(nutrition, analysis, details);

        redisTemplate.opsForValue().set(cacheKey, fullResponse, CACHE_TTL);
        return fullResponse;
    }

    @Transactional
    public NutritionAnalysisResponse getNutritionByRecipeId(Long recipeId, boolean forceRefresh) {
        String cacheKey = CACHE_PREFIX + recipeId;

        if (!forceRefresh) {
            NutritionAnalysisResponse cached =
                    (NutritionAnalysisResponse) redisTemplate.opsForValue().get(cacheKey);
            if (cached != null) {
                log.info("[Nutrition] Cache hit for recipeId={}", recipeId);
                return cached;
            }
        }

        Recipe recipe = recipeRepository.findByIdWithIngredients(recipeId)
                .orElseThrow(() -> ExceptionUtil.notFound(ErrorMessage.RECIPE_NOT_FOUND));

        if (recipe.getIngredients() == null || recipe.getIngredients().isEmpty()) {
            throw ExceptionUtil.badRequest(ErrorMessage.RECIPE_HAS_NO_INGREDIENTS);
        }

        List<RecipeIngredient> ingredients = new ArrayList<>(recipe.getIngredients());
        List<RecipeIngredientNutrition> rinList =
                recipeIngredientNutritionResolver.resolveIngredientNutritions(recipe, ingredients);

        RecipeNutrition nutrition = calculateAndSaveTotalNutrition(recipe, rinList);
        List<IngredientNutritionDetail> details = rinList.stream()
                .map(nutritionMapper::toDetail)
                .toList();

        NutritionAnalysisResponse response = nutritionMapper.toPartialResponse(nutrition, details);
        redisTemplate.opsForValue().set(cacheKey, response, CACHE_TTL);
        return response;
    }

    public NutritionAnalysisResponse calculateMealPlanNutrition(List<MealPlanNutritionIngredientRequest> ingredients) {
        if (ingredients == null || ingredients.isEmpty()) {
            throw ExceptionUtil.badRequest(ErrorMessage.RECIPE_HAS_NO_INGREDIENTS);
        }

        List<IngredientNutritionDetail> details = new ArrayList<>();
        double calories = 0D;
        double protein = 0D;
        double fat = 0D;
        double carb = 0D;
        double fiber = 0D;
        double sugar = 0D;
        double calcium = 0D;
        double sodium = 0D;

        for (MealPlanNutritionIngredientRequest item : ingredients) {
            if (item == null || item.getName() == null || item.getName().isBlank()) {
                continue;
            }

            double grams = calculator.toGrams(item.getQuantity() == null ? 0D : item.getQuantity(), item.getUnit());
            Ingredient ingredient = ingredientRepository.findByNameIgnoreCase(item.getName().trim()).orElse(null);
            IngredientNutrition nutrition = ingredient == null
                    ? null
                    : ingredientNutritionRepository.findByIngredientId(ingredient.getId()).orElse(null);
            double ratio = grams / 100D;
            double itemCalories = nutrition == null ? 0D : calculator.round(calculator.safe(nutrition.getCalories()) * ratio);
            double itemProtein = nutrition == null ? 0D : calculator.round(calculator.safe(nutrition.getProtein()) * ratio);
            double itemFat = nutrition == null ? 0D : calculator.round(calculator.safe(nutrition.getFat()) * ratio);
            double itemCarb = nutrition == null ? 0D : calculator.round(calculator.safe(nutrition.getCarb()) * ratio);
            double itemFiber = nutrition == null ? 0D : calculator.round(calculator.safe(nutrition.getFiber()) * ratio);
            double itemSugar = nutrition == null ? 0D : calculator.round(calculator.safe(nutrition.getSugar()) * ratio);
            double itemCalcium = nutrition == null ? 0D : calculator.round(calculator.safe(nutrition.getCalcium()) * ratio);
            double itemSodium = nutrition == null ? 0D : calculator.round(calculator.safe(nutrition.getSodium()) * ratio);

            calories += itemCalories;
            protein += itemProtein;
            fat += itemFat;
            carb += itemCarb;
            fiber += itemFiber;
            sugar += itemSugar;
            calcium += itemCalcium;
            sodium += itemSodium;

            details.add(IngredientNutritionDetail.builder()
                    .ingredientName(item.getName().trim())
                    .quantity(grams)
                    .unit(item.getUnit())
                    .calories(itemCalories)
                    .protein(itemProtein)
                    .fat(itemFat)
                    .carb(itemCarb)
                    .fiber(itemFiber)
                    .sugar(itemSugar)
                    .calcium(itemCalcium)
                    .sodium(itemSodium)
                    .build());
        }

        return NutritionAnalysisResponse.builder()
                .totalCalories(calculator.round(calories))
                .totalProtein(calculator.round(protein))
                .totalFat(calculator.round(fat))
                .totalCarb(calculator.round(carb))
                .totalFiber(calculator.round(fiber))
                .totalSugar(calculator.round(sugar))
                .totalCalcium(calculator.round(calcium))
                .totalSodium(calculator.round(sodium))
                .healthScore(calculator.round(calculator.calcHealthScore(calories, fat, sodium, protein, fiber, sugar)))
                .ingredientDetails(details)
                .build();
    }

    // Phân tích dinh dưỡng từ recipeId có sẵn trong DB
    public SseEmitter analyzeStreamByRecipeId(Long recipeId, boolean forceRefresh) {
        SseEmitter emitter = new SseEmitter(60_000L);

        // TRƯỚC khi chuyển sang async thread
        String cacheKey = CACHE_PREFIX + recipeId;

        if (!forceRefresh) {
            NutritionAnalysisResponse cached =
                    (NutritionAnalysisResponse) redisTemplate.opsForValue().get(cacheKey);
            if (cached != null) {
                log.info("[Nutrition SSE] Cache hit for recipeId={}", recipeId);
                try {
                    nutritionSseUtils.sendAnalysisData(emitter, cached);
                    nutritionSseUtils.sendComplete(emitter, "Phân tích hoàn tất");
                    emitter.complete();
                } catch (Exception e) {
                    emitter.completeWithError(e);
                }
                return emitter;
            }
        }

        Recipe recipe = recipeRepository.findByIdWithIngredients(recipeId)
                .orElseThrow(() -> ExceptionUtil.notFound(ErrorMessage.RECIPE_NOT_FOUND));

        // Validate ngay trên main thread
        if (recipe.getIngredients() == null || recipe.getIngredients().isEmpty()) {
            try {
                nutritionSseUtils.sendError(emitter, ErrorMessage.RECIPE_HAS_NO_INGREDIENTS.getMessage());
                emitter.complete();
            } catch (Exception e) {
                emitter.completeWithError(e);
            }
            return emitter;
        }

        // Chuyển sang async sau khi đã có đủ data
        CompletableFuture.runAsync(() -> {
            try {
                analyzeStream(recipe, emitter);
            } catch (Exception e) {
                log.error("[Nutrition SSE] Error: {}", e.getMessage(), e);
                nutritionSseUtils.sendError(emitter, e.getMessage());
                emitter.complete();
            }
        }, nutritionAiExecutor);

        return emitter;
    }

    // Phân tích dinh dưỡng từ ảnh món ăn
    public SseEmitter analyzeStreamByImage(MultipartFile imageFile) {
        SseEmitter emitter = new SseEmitter(60_000L);

        CompletableFuture.runAsync(() -> {
            try {
                // Gọi AI với URL trực tiếp, không cache, không base64
                String dishName = reactionClient.getKeyword(imageFile).getData();
                log.info("[Nutrition SSE] Identified dish: {}", dishName);

                if (dishName == null || dishName.isBlank()) {
                    nutritionSseUtils.sendError(emitter, "Không nhận diện được món ăn từ ảnh.");
                    emitter.complete();
                    return;
                }

                String safeDishName = dishName.replaceAll("\\x00", "").trim();
                if (safeDishName.isBlank()) {
                    nutritionSseUtils.sendError(emitter, "Tên món ăn không hợp lệ.");
                    emitter.complete();
                    return;
                }

                Recipe recipe = recipeRepository
                        .findFirstByTitleAndDescriptionWithIngredients(safeDishName, dishName)
                        .orElseThrow(() -> ExceptionUtil.notFound(ErrorMessage.RECIPE_NOT_FOUND));

                String recipeCacheKey = CACHE_PREFIX + recipe.getId();
                NutritionAnalysisResponse cached = (NutritionAnalysisResponse) redisTemplate.opsForValue().get(recipeCacheKey);

                if (cached != null) {
                    log.info("[Nutrition SSE] Cache hit for recipe: {}", recipe.getId());
                    nutritionSseUtils.sendAnalysisData(emitter, cached);
                    nutritionSseUtils.sendComplete(emitter, "Phân tích hoàn tất");
                    emitter.complete();
                    return;
                }

                analyzeStream(recipe, emitter);

            } catch (Exception e) {
                log.error("[Nutrition SSE] Image analyze error: {}", e.getMessage(), e);
                nutritionSseUtils.sendError(emitter, e.getMessage());
                emitter.complete();
            }
        }, nutritionAiExecutor);

        return emitter;
    }


    private void analyzeStream(Recipe recipe, SseEmitter emitter) {
        nutritionSseUtils.sendStatus(emitter, "Đang tính toán dinh dưỡng từng nguyên liệu...");

        List<RecipeIngredient> ingredients = new ArrayList<>(recipe.getIngredients());
        if (ingredients.isEmpty()) {
            throw new IllegalStateException("Recipe has no ingredients: " + recipe.getId());
        }

        List<RecipeIngredientNutrition> rinList = recipeIngredientNutritionResolver.resolveIngredientNutritions(recipe, ingredients);

        // Tính nutrition tổng → gửi ngay, không chờ AI
        RecipeNutrition nutrition = calculateAndSaveTotalNutrition(recipe, rinList);

        List<IngredientNutritionDetail> details = rinList.stream()
                .map(nutritionMapper::toDetail)
                .toList();

        // Gửi partial response (chưa có AI analysis)
        NutritionAnalysisResponse partialResponse = nutritionMapper.toPartialResponse(nutrition, details);
        nutritionSseUtils.sendNutritionData(emitter, partialResponse);

        //Chờ AI analysis → gửi khi xong
        RecipeNutritionAnalysis analysis;
        try {
            analysis = CompletableFuture
                    .supplyAsync(() -> aiAnalyzeAndSave(recipe, nutrition), nutritionAiExecutor)
                    .get(15, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("[Nutrition SSE] AI analysis timed out, using fallback");
            analysis = buildFallbackAnalysis(recipe, nutrition);
        }

        // Gửi full response (có AI analysis)
        NutritionAnalysisResponse fullResponse = nutritionMapper.toResponse(nutrition, analysis, details);
        nutritionSseUtils.sendAnalysisData(emitter, fullResponse);

        // Lưu cache và kết thúc
        String cacheKey = CACHE_PREFIX + recipe.getId();
        redisTemplate.opsForValue().set(cacheKey, fullResponse, CACHE_TTL);

        nutritionSseUtils.sendComplete(emitter, "Phân tích dinh dưỡng hoàn tất!");
        emitter.complete();
    }

    private RecipeNutrition calculateAndSaveTotalNutrition(
            Recipe recipe,
            List<RecipeIngredientNutrition> rinList
    ) {
        RecipeNutrition totals = calculator.calcTotal(rinList);

        RecipeNutrition nutrition = recipeNutritionRepository
                .findByRecipeId(recipe.getId())
                .orElse(RecipeNutrition.builder().recipe(recipe).build());

        nutrition.setTotalCalories(calculator.round(totals.getTotalCalories()));
        nutrition.setTotalProtein(calculator.round(totals.getTotalProtein()));
        nutrition.setTotalFat(calculator.round(totals.getTotalFat()));
        nutrition.setTotalCarb(calculator.round(totals.getTotalCarb()));
        nutrition.setTotalFiber(calculator.round(totals.getTotalFiber()));
        nutrition.setTotalSugar(calculator.round(totals.getTotalSugar()));
        nutrition.setTotalCalcium(calculator.round(totals.getTotalCalcium()));
        nutrition.setTotalSodium(calculator.round(totals.getTotalSodium()));
        nutrition.setHealthScore(calculator.round(totals.getHealthScore()));

        return recipeNutritionRepository.save(nutrition);
    }

    private RecipeNutritionAnalysis aiAnalyzeAndSave(Recipe recipe, RecipeNutrition nutrition) {
        String prompt = promptBuilder.buildPrompt(recipe.getTitle(), nutrition);

        String aiJson;
        try {
            aiJson = aiService.analyzeNutrition(prompt);
        } catch (Exception e) {
            log.error("[Nutrition] AI analysis failed: {}", e.getMessage());
            aiJson = promptBuilder.fallbackJson(nutrition);
        }

        AINutritionResult aiResult = promptBuilder.parseResult(aiJson);

        RecipeNutritionAnalysis analysis = recipeNutritionAnalysisRepository.findByRecipeId(recipe.getId())
                .orElse(RecipeNutritionAnalysis.builder().recipe(recipe).build());

        analysis.setSummary(aiResult.getSummary());
        analysis.setHealthLevel(HealthLevel.valueOf(aiResult.getHealthLevel()));
        analysis.setRecommendation(aiResult.getRecommendation());

        return recipeNutritionAnalysisRepository.save(analysis);
    }

    private RecipeNutritionAnalysis buildFallbackAnalysis(Recipe recipe, RecipeNutrition nutrition) {
        RecipeNutritionAnalysis analysis = recipeNutritionAnalysisRepository
                .findByRecipeId(recipe.getId())
                .orElse(RecipeNutritionAnalysis.builder().recipe(recipe).build());
        String level = nutrition.getHealthScore() >= 70 ? HealthLevel.GOOD.name()
                : nutrition.getHealthScore() >= 40 ? HealthLevel.FAIR.name()
                : HealthLevel.POOR.name();
        analysis.setSummary("Phân tích tự động.");
        analysis.setHealthLevel(HealthLevel.valueOf(level));
        analysis.setRecommendation("Cân bằng khẩu phần ăn.");
        return recipeNutritionAnalysisRepository.save(analysis);
    }

    public void evictCache(Long recipeId) {
        redisTemplate.delete(CACHE_PREFIX + recipeId);
    }
}
