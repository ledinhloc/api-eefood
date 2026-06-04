package com.eefood.reactionservice.mealplan.service;

import com.eefood.reactionservice.mealplan.dto.ai.MealPlanAiCandidate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;

@Service
public class MealPlanNutritionScoringService {

    public int score(MealPlanAiCandidate candidate, String goal, List<String> healthConditions) {
        if (candidate.getNutrition() == null) {
            return 0;
        }

        String normalizedGoal = normalize(goal);
        int score = 0;

        if (hasAnyTextMatch(normalizedGoal, List.of("giảm cân", "giam can", "eat clean", "healthy", "ăn kiêng", "an kieng"))) {
            score += scoreWeightLossNutrition(candidate);
        }
        if (hasAnyTextMatch(normalizedGoal, List.of("tăng cơ", "tang co", "protein", "muscle"))) {
            score += scoreHighProteinNutrition(candidate);
        }
        if (hasAnyTextMatch(normalizedGoal, List.of("ít đường", "it duong", "tiểu đường", "tieu duong", "low sugar"))
                || hasAnyTextMatch(healthConditions, List.of("tiểu đường", "tieu duong", "diabetes"))) {
            score += scoreLowSugarNutrition(candidate);
        }
        if (hasAnyTextMatch(healthConditions, List.of("cao huyết áp", "cao huyet ap", "hypertension"))) {
            score += scoreLowSodiumNutrition(candidate);
        }
        if (hasAnyTextMatch(healthConditions, List.of("mỡ máu", "mo mau", "cholesterol", "tim mạch", "tim mach"))) {
            score += scoreLowFatNutrition(candidate);
        }

        return score;
    }

    private int scoreWeightLossNutrition(MealPlanAiCandidate candidate) {
        double calories = nutritionValue(candidate.getNutrition().getTotalCalories());
        double protein = nutritionValue(candidate.getNutrition().getTotalProtein());
        double fiber = nutritionValue(candidate.getNutrition().getTotalFiber());

        int score = 0;
        score += calories <= 400 ? 8 : calories <= 600 ? 4 : calories >= 800 ? -8 : 0;
        score += protein >= 25 ? 6 : protein >= 15 ? 3 : 0;
        score += fiber >= 5 ? 6 : fiber >= 3 ? 3 : 0;
        return score;
    }

    private int scoreHighProteinNutrition(MealPlanAiCandidate candidate) {
        double protein = nutritionValue(candidate.getNutrition().getTotalProtein());
        return protein >= 35 ? 12 : protein >= 25 ? 8 : protein >= 15 ? 4 : 0;
    }

    private int scoreLowSugarNutrition(MealPlanAiCandidate candidate) {
        double sugar = nutritionValue(candidate.getNutrition().getTotalSugar());
        double fiber = nutritionValue(candidate.getNutrition().getTotalFiber());

        int score = 0;
        score += sugar <= 5 ? 10 : sugar <= 10 ? 5 : sugar >= 20 ? -10 : 0;
        score += fiber >= 5 ? 5 : fiber >= 3 ? 2 : 0;
        return score;
    }

    private int scoreLowSodiumNutrition(MealPlanAiCandidate candidate) {
        double sodium = nutritionValue(candidate.getNutrition().getTotalSodium());
        return sodium <= 400 ? 8 : sodium <= 700 ? 4 : sodium >= 1000 ? -8 : 0;
    }

    private int scoreLowFatNutrition(MealPlanAiCandidate candidate) {
        double fat = nutritionValue(candidate.getNutrition().getTotalFat());
        return fat <= 15 ? 8 : fat <= 25 ? 4 : fat >= 35 ? -8 : 0;
    }

    private boolean hasAnyTextMatch(String text, List<String> keywords) {
        return keywords.stream().anyMatch(keyword -> isTextMatch(text, keyword));
    }

    private boolean hasAnyTextMatch(List<String> values, List<String> keywords) {
        return values.stream().anyMatch(value -> hasAnyTextMatch(value, keywords));
    }

    private boolean isTextMatch(String text, String keyword) {
        String normalizedText = normalize(text);
        String normalizedKeyword = normalize(keyword);
        return !normalizedText.isBlank()
                && !normalizedKeyword.isBlank()
                && (normalizedText.contains(normalizedKeyword) || normalizedKeyword.contains(normalizedText));
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private double nutritionValue(Double value) {
        return value == null ? 0d : value;
    }
}
