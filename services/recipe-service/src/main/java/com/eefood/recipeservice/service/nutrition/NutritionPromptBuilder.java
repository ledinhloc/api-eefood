package com.eefood.recipeservice.service.nutrition;

import com.eefood.recipeservice.dto.response.AINutritionResult;
import com.eefood.recipeservice.enums.HealthLevel;
import com.eefood.recipeservice.model.RecipeNutrition;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class NutritionPromptBuilder {
    private final ObjectMapper objectMapper;

    public String buildPrompt(String title, RecipeNutrition n) {
        return """
            Tên món: %s
            Calories: %.1f kcal | Protein: %.1f g | Fat: %.1f g | Carb: %.1f g
            Fiber: %.1f g | Sugar: %.1f g | Calcium: %.1f mg | Sodium: %.1f mg | Health Score: %.1f/100
            """.formatted(
                title,
                safe(n.getTotalCalories()), safe(n.getTotalProtein()),
                safe(n.getTotalFat()),       safe(n.getTotalCarb()),
                safe(n.getTotalFiber()),     safe(n.getTotalSugar()),
                safe(n.getTotalCalcium()),   safe(n.getTotalSodium()),
                safe(n.getHealthScore())
        );
    }

    public AINutritionResult parseResult(String json) {
        try {
            String clean = json.replaceAll("```json|```", "").trim();
            return objectMapper.readValue(clean, AINutritionResult.class);
        } catch (JsonProcessingException e) {
            log.warn("[Nutrition] Failed to parse AI result, using fallback. Error: {}", e.getMessage());
            return AINutritionResult.builder()
                    .summary("Không thể phân tích dinh dưỡng chi tiết.")
                    .healthLevel(HealthLevel.FAIR.name())
                    .recommendation("Hãy cân bằng khẩu phần ăn hàng ngày.")
                    .build();
        }
    }

    public String fallbackJson(RecipeNutrition n) {
        String level = n.getHealthScore() >= 70 ? HealthLevel.GOOD.name()
                : n.getHealthScore() >= 40 ? HealthLevel.FAIR.name()
                : HealthLevel.POOR.name();
        return """
            {"summary":"Phân tích tự động.","healthLevel":"%s","recommendation":"Cân bằng khẩu phần ăn."}
            """.formatted(level);
    }

    private double safe(Double v) { return v != null ? v : 0.0; }
}
