package com.eefood.recipeservice.service.nutrition;

import com.eefood.recipeservice.dto.response.AINutritionResult;
import com.eefood.recipeservice.dto.response.IngredientNutritionDetail;
import com.eefood.recipeservice.dto.response.NutritionAnalysisResponse;
import com.eefood.recipeservice.enums.ErrorMessage;
import com.eefood.recipeservice.enums.HealthLevel;
import com.eefood.recipeservice.exception.ExceptionUtil;
import com.eefood.recipeservice.mapper.NutritionMapper;
import com.eefood.recipeservice.model.*;
import com.eefood.recipeservice.repository.RecipeRepository;
import com.eefood.recipeservice.repository.nutrition.*;
import com.eefood.recipeservice.util.ImageUtils;
import com.eefood.recipeservice.util.NutritionSseUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class NutritionAnalysisService {
    private final RecipeRepository recipeRepository;
    private final IngredientNutritionRepository ingredientNutritionRepository;
    private final FoodNutritionDatasetRepository foodDatasetRepository;
    private final RecipeIngredientNutritionRepository recipeIngredientNutritionRepository;
    private final RecipeNutritionRepository recipeNutritionRepository;
    private final RecipeNutritionAnalysisRepository recipeNutritionAnalysisRepository;
    private final AIService aiService;
    private final NutritionMapper nutritionMapper;
    private final ObjectMapper objectMapper;
    private final NutritionCalculator calculator;
    private final NutritionPromptBuilder promptBuilder;
    private final RedisTemplate<String, Object> redisTemplate;
    private final Executor nutritionAiExecutor;
    private static final String CACHE_PREFIX = "nutrition:recipe:";
    private static final Duration CACHE_TTL = Duration.ofHours(24);
    private final NutritionSseUtils nutritionSseUtils;


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
    public SseEmitter analyzeStreamByImage(String imageUrl) {
        SseEmitter emitter = new SseEmitter(60_000L);

        CompletableFuture.runAsync(() -> {
            try {
                nutritionSseUtils.sendStatus(emitter, "Đang nhận dạng món ăn từ ảnh...");
                String base64Image = ImageUtils.downloadAndEncodeImage(imageUrl);
                String dishName = aiService.identifyDishFromImage(base64Image);
                log.info("[Nutrition SSE] Identified dish: {}", dishName);

                Recipe recipe = recipeRepository
                        .findFirstByTitleWithIngredients(dishName)
                        .orElseThrow(() -> ExceptionUtil.notFound(ErrorMessage.RECIPE_NOT_FOUND));

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

        List<RecipeIngredientNutrition> rinList = resolveIngredientNutritions(recipe, ingredients);

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

    private List<RecipeIngredientNutrition> resolveIngredientNutritions(
            Recipe recipe, List<RecipeIngredient> ingredients) {

        recipeIngredientNutritionRepository.deleteByRecipeId(recipe.getId());

        List<Long> ingredientIds = ingredients.stream()
                .map(ri -> ri.getIngredient().getId())
                .toList();

        CompletableFuture<Map<Long, String>> normalizeFuture =
                CompletableFuture.supplyAsync(
                        () -> batchNormalizeIngredientNames(ingredients),
                        nutritionAiExecutor);

        CompletableFuture<Map<Long, IngredientNutrition>> existingFuture =
                CompletableFuture.supplyAsync(
                        () -> ingredientNutritionRepository
                                .findByIngredientIdIn(ingredientIds)
                                .stream()
                                .collect(Collectors.toMap(
                                        n -> n.getIngredient().getId(),
                                        Function.identity())),
                        nutritionAiExecutor);

        // Khai báo raw trước, gán sau try/catch
        Map<Long, String> rawKeywords;
        Map<Long, IngredientNutrition> rawNutritions;
        try {
            rawKeywords    = normalizeFuture.get(10, TimeUnit.SECONDS);
            rawNutritions  = existingFuture.get(5, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("[Nutrition] Parallel init failed, falling back: {}", e.getMessage());
            rawKeywords    = fallbackToOriginalNames(ingredients);
            rawNutritions  = ingredientNutritionRepository
                    .findByIngredientIdIn(ingredientIds)
                    .stream()
                    .collect(Collectors.toMap(
                            n -> n.getIngredient().getId(),
                            Function.identity()));
        }

        // Gán vào final map — an toàn để dùng trong lambda
        final Map<Long, String> normalizedKeywords = rawKeywords;
        final Map<Long, IngredientNutrition> existingNutritions = new HashMap<>(rawNutritions);

        // Xác định các ingredient chưa có nutrition trong DB
        Map<Long, String> missingKeywords = new LinkedHashMap<>();
        for (RecipeIngredient ri : ingredients) {
            Long id = ri.getIngredient().getId();
            if (!existingNutritions.containsKey(id)) {
                missingKeywords.put(id,
                        normalizedKeywords.getOrDefault(id, ri.getIngredient().getName()));
            }
        }

        // Batch query dataset cho các keyword còn thiếu
        if (!missingKeywords.isEmpty()) {
            Map<String, FoodNutritionDataset> datasetByKeyword = new HashMap<>();
            String keywordsJoined = String.join(",", missingKeywords.values());
            foodDatasetRepository
                    .findByFoodNameViContainingAnyKeyword(keywordsJoined)
                    .forEach(d -> datasetByKeyword.putIfAbsent(d.getFoodNameVi().toLowerCase(), d));

            List<IngredientNutrition> toSave = new ArrayList<>();
            for (Map.Entry<Long, String> entry : missingKeywords.entrySet()) {
                Long ingredientId = entry.getKey();
                String keyword    = entry.getValue().toLowerCase();

                FoodNutritionDataset source = datasetByKeyword.entrySet().stream()
                        .filter(e -> e.getKey().contains(keyword))
                        .map(Map.Entry::getValue)
                        .findFirst()
                        .orElse(null);

                if (source == null) continue;

                Ingredient ingredient = ingredients.stream()
                        .filter(ri -> ri.getIngredient().getId().equals(ingredientId))
                        .map(RecipeIngredient::getIngredient)
                        .findFirst()
                        .orElseThrow();

                toSave.add(buildIngredientNutrition(ingredient, source));
            }

            if (!toSave.isEmpty()) {
                ingredientNutritionRepository.saveAll(toSave)
                        .forEach(n -> existingNutritions.put(n.getIngredient().getId(), n));
            }
        }

        // Build RecipeIngredientNutrition list và batch save
        List<RecipeIngredientNutrition> rinList = new ArrayList<>();
        for (RecipeIngredient ri : ingredients) {
            Long id = ri.getIngredient().getId();
            IngredientNutrition nutrition = existingNutritions.get(id);
            if (nutrition == null) {
                log.warn("[Nutrition] No nutrition data found for ingredient id={}, keyword='{}'",
                        id, normalizedKeywords.getOrDefault(id, ri.getIngredient().getName()));
                continue;
            }
            rinList.add(buildRecipeIngredientNutrition(recipe, ri, nutrition));
        }

        return recipeIngredientNutritionRepository.saveAll(rinList);
    }


    private RecipeIngredientNutrition buildRecipeIngredientNutrition(
            Recipe recipe,
            RecipeIngredient ri,
            IngredientNutrition nutrition
    ) {
        double grams = calculator.toGrams(ri.getQuantity(), ri.getUnit());
        double ratio = grams / 100.0;

        return RecipeIngredientNutrition.builder()
                .recipe(recipe)
                .ingredient(ri.getIngredient())
                .quantity(grams)
                .calories(calculator.round(calculator.safe(nutrition.getCalories()) * ratio))
                .protein(calculator.round(calculator.safe(nutrition.getProtein())  * ratio))
                .fat(calculator.round(calculator.safe(nutrition.getFat())          * ratio))
                .carb(calculator.round(calculator.safe(nutrition.getCarb())        * ratio))
                .fiber(calculator.round(calculator.safe(nutrition.getFiber())      * ratio))
                .sugar(calculator.round(calculator.safe(nutrition.getSugar())      * ratio))
                .calcium(calculator.round(calculator.safe(nutrition.getCalcium())  * ratio))
                .sodium(calculator.round(calculator.safe(nutrition.getSodium())    * ratio))
                .build();
    }

    private IngredientNutrition buildIngredientNutrition(
            Ingredient ingredient, FoodNutritionDataset source) {
        return IngredientNutrition.builder()
                .ingredient(ingredient)
                .calories(source.getEnergy())
                .protein(source.getProtein())
                .fat(source.getFat())
                .carb(source.getCarb())
                .fiber(source.getFiber())
                .sugar(source.getSugar())
                .calcium(source.getCalcium())
                .sodium(source.getSodium())
                .sourceFood(source)
                .build();
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

    private Map<Long, String> batchNormalizeIngredientNames(List<RecipeIngredient> ingredients) {
        List<String> names = ingredients.stream()
                .map(ri -> ri.getIngredient().getName())
                .toList();

        try {
            String namesJson = objectMapper.writeValueAsString(names);
            String aiResponse = aiService.normalizeIngredientNames(namesJson);

            // Parse kết quả AI trả về
            String cleanJson = aiResponse.replaceAll("```json|```", "").trim();
            List<String> normalizedNames = objectMapper.readValue(cleanJson, new TypeReference<List<String>>() {});

            // Validate: AI phải trả về đúng số lượng
            if (normalizedNames.size() != ingredients.size()) {
                log.warn("[Nutrition] AI returned {} names but expected {}. Falling back to original names.",
                        normalizedNames.size(), ingredients.size());
                return fallbackToOriginalNames(ingredients);
            }

            // Map ingredientId → normalizedKeyword
            Map<Long, String> result = new LinkedHashMap<>();
            for (int i = 0; i < ingredients.size(); i++) {
                String normalized = normalizedNames.get(i);
                result.put(
                        ingredients.get(i).getIngredient().getId(),
                        (normalized != null && !normalized.isBlank()) ? normalized.trim() : names.get(i)
                );
            }
            return result;

        } catch (Exception e) {
            log.warn("[Nutrition] Batch normalize failed: {}. Falling back to original names.", e.getMessage());
            return fallbackToOriginalNames(ingredients);
        }
    }

    private Map<Long, String> fallbackToOriginalNames(List<RecipeIngredient> ingredients) {
        return ingredients.stream().collect(Collectors.toMap(
                ri -> ri.getIngredient().getId(),
                ri -> ri.getIngredient().getName(),
                (a, b) -> a,
                LinkedHashMap::new
        ));
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
