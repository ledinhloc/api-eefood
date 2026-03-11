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
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
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

    // Phân tích dinh dưỡng từ recipeId có sẵn trong DB
    public NutritionAnalysisResponse analyzeByRecipeId(Long recipeId) {
        Recipe recipe = recipeRepository.findByIdAndIsDeletedFalse(recipeId)
                .orElseThrow(() -> ExceptionUtil.notFound(ErrorMessage.RECIPE_NOT_FOUND));
        return analyze(recipe);
    }

    // Phân tích dinh dưỡng từ ảnh món ăn
    public NutritionAnalysisResponse analyzeByImage(String imageUrl) {
        String base64Image = ImageUtils.downloadAndEncodeImage(imageUrl);

        // AI nhận dạng tên món ăn từ ảnh
        String dishName = aiService.identifyDishFromImage(base64Image);
        log.info("[Nutrition] Identified dish from image: {}", dishName);

        // Tìm recipe phù hợp nhất theo tên món
        Recipe recipe = recipeRepository
                .findFirstByTitleContainingIgnoreCaseAndIsDeletedFalse(dishName)
                .orElseThrow(() -> ExceptionUtil.notFound(ErrorMessage.RECIPE_NOT_FOUND));

        return analyze(recipe);
    }


    private NutritionAnalysisResponse analyze(Recipe recipe) {
        List<RecipeIngredient> ingredients = new ArrayList<>(recipe.getIngredients());

        if (ingredients.isEmpty()) {
            throw new IllegalStateException("Recipe has no ingredients: " + recipe.getId());
        }

        // Chuẩn hóa tên nguyên liệu + mapping dinh dưỡng
        List<RecipeIngredientNutrition> rinList = resolveIngredientNutritions(recipe, ingredients);

        // Tính tổng dinh dưỡng
        RecipeNutrition nutrition = calculateAndSaveTotalNutrition(recipe, rinList);

        // AI phân tích + lưu kết quả
        RecipeNutritionAnalysis analysis = aiAnalyzeAndSave(recipe, nutrition);

        // Build response
        List<IngredientNutritionDetail> details = rinList.stream()
                .map(nutritionMapper::toDetail)
                .toList();

        return nutritionMapper.toResponse(nutrition, analysis, details);
    }

    private List<RecipeIngredientNutrition> resolveIngredientNutritions(Recipe recipe, List<RecipeIngredient> ingredients) {
        recipeIngredientNutritionRepository.deleteByRecipeId(recipe.getId());

        Map<Long, String> normalizedKeywords = batchNormalizeIngredientNames(ingredients);

        List<RecipeIngredientNutrition> result = new ArrayList<>();

        for (RecipeIngredient ri : ingredients) {
            String keyword = normalizedKeywords.getOrDefault(ri.getIngredient().getId(), ri.getIngredient().getName());
            log.info("[Nutrition] '{}' → keyword: '{}'", ri.getIngredient().getName(), keyword);

            IngredientNutrition nutrition = getOrCreateIngredientNutrition(ri.getIngredient(), keyword);
            if (nutrition == null) {
                log.warn("[Nutrition] No nutrition data found for keyword: '{}'", keyword);
                continue;
            }

            RecipeIngredientNutrition rin = buildRecipeIngredientNutrition(recipe, ri, nutrition);
            result.add(recipeIngredientNutritionRepository.save(rin));
        }

        return result;
    }

    private IngredientNutrition getOrCreateIngredientNutrition(Ingredient ingredient, String keyword) {
        // Đã có mapping rồi => dùng luôn
        Optional<IngredientNutrition> existing = ingredientNutritionRepository.findByIngredientId(ingredient.getId());
        if (existing.isPresent()) return existing.get();

        // Tìm trong FoodNutritionDataset theo keyword
        List<FoodNutritionDataset> candidates = foodDatasetRepository.findByFoodNameViContainingIgnoreCase(keyword);
        if (candidates.isEmpty()) return null;

        // Lấy kết quả khớp nhất (đơn giản: phần tử đầu tiên)
        FoodNutritionDataset source = candidates.get(0);

        // Tạo IngredientNutrition mới từ dataset
        IngredientNutrition newNutrition = IngredientNutrition.builder()
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

        return ingredientNutritionRepository.save(newNutrition);
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
        String prompt = promptBuilder.buildPrompt   (recipe.getTitle(), nutrition);

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

}
