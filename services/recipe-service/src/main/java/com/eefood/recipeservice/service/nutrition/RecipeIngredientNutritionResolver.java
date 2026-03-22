package com.eefood.recipeservice.service.nutrition;

import com.eefood.recipeservice.model.*;
import com.eefood.recipeservice.repository.nutrition.FoodNutritionDatasetRepository;
import com.eefood.recipeservice.repository.nutrition.IngredientNutritionRepository;
import com.eefood.recipeservice.repository.nutrition.RecipeIngredientNutritionRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class RecipeIngredientNutritionResolver {
    private final IngredientNutritionRepository ingredientNutritionRepository;
    private final FoodNutritionDatasetRepository foodDatasetRepository;
    private final RecipeIngredientNutritionRepository recipeIngredientNutritionRepository;
    private final NutritionCalculator calculator;
    private final AIService aiService;

    private final Executor nutritionAiExecutor;
    private final ObjectMapper objectMapper;

    @Transactional
    public List<RecipeIngredientNutrition> resolveIngredientNutritions(
            Recipe recipe, List<RecipeIngredient> ingredients) {

        List<RecipeIngredientNutrition> existing =
                recipeIngredientNutritionRepository.findByRecipeId(recipe.getId());

        if (existing.size() == ingredients.size()) {
            log.info("[Nutrition] Reusing existing RecipeIngredientNutrition for recipeId={}",
                    recipe.getId());
            return existing;
        }

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
            rawKeywords    = normalizeFuture.get(15, TimeUnit.SECONDS);
            rawNutritions  = existingFuture.get(8, TimeUnit.SECONDS);
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
            String keywordsJoined = String.join("|", missingKeywords.values());

            log.debug("[Nutrition] Querying dataset with keywords: {}", keywordsJoined);

            // Map: foodNameVi lowercase → dataset
            List<FoodNutritionDataset> datasetResults =
                    foodDatasetRepository.findByFoodNameViContainingAnyKeyword(keywordsJoined);

            log.debug("[Nutrition] Dataset returned {} rows", datasetResults.size());

            List<IngredientNutrition> toSave = new ArrayList<>();
            for (Map.Entry<Long, String> entry : missingKeywords.entrySet()) {
                Long ingredientId = entry.getKey();
                String keyword = entry.getValue().toLowerCase().trim();

                FoodNutritionDataset source = datasetResults.stream()
                        .filter(d -> {
                            String datasetName = d.getFoodNameVi().toLowerCase().trim();
                            return datasetName.contains(keyword) || keyword.contains(datasetName);
                        })
                        .findFirst()
                        .orElse(null);

                if (source == null) {
                    log.warn("[Nutrition] No dataset match for ingredient id={}, keyword='{}'",
                            ingredientId, keyword);
                    continue;
                }

                log.debug("[Nutrition] Matched ingredient id={}, keyword='{}' → dataset='{}'",
                        ingredientId, keyword, source.getFoodNameVi());

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

    private Map<Long, String> batchNormalizeIngredientNames(List<RecipeIngredient> ingredients) {
        Map<Long, String> result = new LinkedHashMap<>();
        List<RecipeIngredient> needsAI = new ArrayList<>();

        for (RecipeIngredient ri : ingredients) {
            String name = ri.getIngredient().getName().trim();
            if (isAlreadyNormalized(name)) {
                result.put(ri.getIngredient().getId(), name);
            } else {
                needsAI.add(ri);
            }
        }

        if (!needsAI.isEmpty()) {
            List<String> names = needsAI.stream()
                    .map(ri -> ri.getIngredient().getName())
                    .toList();
            try {
                String namesJson = objectMapper.writeValueAsString(names);
                String aiResponse = aiService.normalizeIngredientNames(namesJson);
                String cleanJson = aiResponse.replaceAll("```json|```", "").trim();
                List<String> normalizedNames = objectMapper.readValue(cleanJson, new TypeReference<List<String>>() {});

                if (normalizedNames.size() == needsAI.size()) {
                    for (int i = 0; i < needsAI.size(); i++) {
                        String normalized = normalizedNames.get(i);
                        result.put(
                                needsAI.get(i).getIngredient().getId(),
                                (normalized != null && !normalized.isBlank()) ? normalized.trim() : names.get(i)
                        );
                    }
                } else {
                    needsAI.forEach(ri -> result.put(ri.getIngredient().getId(), ri.getIngredient().getName()));
                }
            } catch (Exception e) {
                log.warn("[Nutrition] Normalize failed: {}. Fallback to originals.", e.getMessage());
                needsAI.forEach(ri -> result.put(ri.getIngredient().getId(), ri.getIngredient().getName()));
            }
        }

        return result;
    }

    private Map<Long, String> fallbackToOriginalNames(List<RecipeIngredient> ingredients) {
        return ingredients.stream().collect(Collectors.toMap(
                ri -> ri.getIngredient().getId(),
                ri -> ri.getIngredient().getName(),
                (a, b) -> a,
                LinkedHashMap::new
        ));
    }

    private boolean isAlreadyNormalized(String name) {
        long wordCount = Arrays.stream(name.split("\\s+")).count();
        boolean hasParentheses = name.contains("(") || name.contains(")");
        boolean hasComma = name.contains(",");
        return wordCount <= 3 && !hasParentheses && !hasComma;
    }
}
