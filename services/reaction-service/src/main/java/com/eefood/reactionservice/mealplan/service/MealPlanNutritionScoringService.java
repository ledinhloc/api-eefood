package com.eefood.reactionservice.mealplan.service;

import com.eefood.reactionservice.mealplan.dto.ai.MealPlanAiCandidate;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;

@Service
public class MealPlanNutritionScoringService {

    /**
     * Tính điểm dinh dưỡng tổng hợp theo chất lượng nền, mục tiêu ăn uống và bệnh nền của user.
     */
    public int score(MealPlanAiCandidate candidate, String goal, List<String> healthConditions) {
        if (candidate.getNutrition() == null) {
            return 0;
        }

        String normalizedGoal = normalize(goal);
        int score = scoreBaseNutrition(candidate);

        if (hasAnyTextMatch(normalizedGoal, List.of(
                "giam can", "giam mo", "dot mo", "cat calories", "calorie deficit",
                "eat clean", "healthy", "an kieng", "giu dang", "thanh loc", "lean"
        ))) {
            score += scoreWeightLossNutrition(candidate);
        }
        if (hasAnyTextMatch(normalizedGoal, List.of(
                "tang co", "xay co", "protein", "high protein", "muscle", "gym",
                "tap gym", "the hinh", "fitness", "lean bulk"
        ))) {
            score += scoreHighProteinNutrition(candidate);
        }
        if (hasAnyTextMatch(normalizedGoal, List.of(
                "tang can", "gain weight", "bulk", "bulking", "tang khoi luong",
                "tang calories", "calorie surplus"
        ))) {
            score += scoreWeightGainNutrition(candidate);
        }
        if (hasAnyTextMatch(normalizedGoal, List.of(
                "duy tri", "duy tri can nang", "can bang", "balanced", "balance",
                "maintenance", "on dinh", "khoe manh", "lanh manh", "suc khoe"
        ))) {
            score += scoreBalancedNutrition(candidate);
        }
        if (hasAnyTextMatch(normalizedGoal, List.of(
                "it duong", "giam duong", "kiem soat duong", "duong huyet",
                "tieu duong", "low sugar", "diabetes", "diabetic", "insulin"
        ))
                || hasAnyTextMatch(healthConditions, List.of(
                "tieu duong", "diabetes", "diabetic", "duong huyet", "insulin"
        ))) {
            score += scoreLowSugarNutrition(candidate);
        }
        if (hasAnyTextMatch(healthConditions, List.of(
                "cao huyet ap", "huyet ap cao", "hypertension", "blood pressure",
                "an nhat", "it muoi", "low sodium", "giam muoi"
        ))) {
            score += scoreLowSodiumNutrition(candidate);
        }
        if (hasAnyTextMatch(healthConditions, List.of(
                "mo mau", "cholesterol", "tim mach", "heart", "cardio",
                "cardiovascular", "roi loan lipid", "triglyceride", "it beo", "low fat"
        ))) {
            score += scoreLowFatNutrition(candidate);
        }

        return score;
    }

    /**
     * Chấm điểm nền cho mọi món: calories hợp lý, protein/fiber tốt, sugar/sodium thấp và healthScore cao.
     */
    private int scoreBaseNutrition(MealPlanAiCandidate candidate) {
        double calories = nutritionValue(candidate.getNutrition().getTotalCalories());
        double protein = nutritionValue(candidate.getNutrition().getTotalProtein());
        double fiber = nutritionValue(candidate.getNutrition().getTotalFiber());
        double sugar = nutritionValue(candidate.getNutrition().getTotalSugar());
        double sodium = nutritionValue(candidate.getNutrition().getTotalSodium());
        double healthScore = nutritionValue(candidate.getNutrition().getHealthScore());

        int score = 0;
        score += calories >= 300 && calories <= 750 ? 4 : calories >= 1000 ? -6 : 0;
        score += protein >= 15 ? 4 : protein >= 8 ? 2 : 0;
        score += fiber >= 5 ? 4 : fiber >= 3 ? 2 : 0;
        score += sugar <= 10 ? 3 : sugar >= 25 ? -6 : 0;
        score += sodium <= 700 ? 3 : sodium >= 1200 ? -6 : 0;
        score += healthScore >= 80 ? 6 : healthScore >= 60 ? 3 : healthScore > 0 && healthScore < 40 ? -6 : 0;
        return score;
    }

    /**
     * Ưu tiên món ít calories nhưng vẫn có protein và fiber cho mục tiêu giảm cân/ăn kiêng.
     */
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

    /**
     * Ưu tiên món giàu protein cho mục tiêu tăng cơ hoặc tập luyện.
     */
    private int scoreHighProteinNutrition(MealPlanAiCandidate candidate) {
        double protein = nutritionValue(candidate.getNutrition().getTotalProtein());
        return protein >= 35 ? 12 : protein >= 25 ? 8 : protein >= 15 ? 4 : 0;
    }

    /**
     * Ưu tiên món nhiều calories vừa phải, đủ protein và không quá nhiều đường cho mục tiêu tăng cân.
     */
    private int scoreWeightGainNutrition(MealPlanAiCandidate candidate) {
        double calories = nutritionValue(candidate.getNutrition().getTotalCalories());
        double protein = nutritionValue(candidate.getNutrition().getTotalProtein());
        double sugar = nutritionValue(candidate.getNutrition().getTotalSugar());

        int score = 0;
        score += calories >= 650 && calories <= 950 ? 10 : calories >= 500 ? 5 : 0;
        score += protein >= 30 ? 8 : protein >= 20 ? 5 : protein >= 12 ? 2 : 0;
        score += sugar <= 15 ? 3 : sugar >= 30 ? -6 : 0;
        return score;
    }

    /**
     * Ưu tiên món cân bằng cho mục tiêu duy trì sức khỏe hoặc ăn uống ổn định.
     */
    private int scoreBalancedNutrition(MealPlanAiCandidate candidate) {
        double calories = nutritionValue(candidate.getNutrition().getTotalCalories());
        double protein = nutritionValue(candidate.getNutrition().getTotalProtein());
        double fat = nutritionValue(candidate.getNutrition().getTotalFat());
        double fiber = nutritionValue(candidate.getNutrition().getTotalFiber());
        double sugar = nutritionValue(candidate.getNutrition().getTotalSugar());
        double sodium = nutritionValue(candidate.getNutrition().getTotalSodium());

        int score = 0;
        score += calories >= 350 && calories <= 700 ? 8 : calories >= 900 ? -6 : 0;
        score += protein >= 15 && protein <= 40 ? 5 : protein > 40 ? 2 : 0;
        score += fat <= 25 ? 4 : fat >= 40 ? -6 : 0;
        score += fiber >= 4 ? 4 : fiber >= 2 ? 2 : 0;
        score += sugar <= 15 ? 4 : sugar >= 30 ? -6 : 0;
        score += sodium <= 800 ? 4 : sodium >= 1200 ? -6 : 0;
        return score;
    }

    /**
     * Ưu tiên món ít đường, nhiều fiber cho người cần kiểm soát đường huyết hoặc tiểu đường.
     */
    private int scoreLowSugarNutrition(MealPlanAiCandidate candidate) {
        double sugar = nutritionValue(candidate.getNutrition().getTotalSugar());
        double fiber = nutritionValue(candidate.getNutrition().getTotalFiber());

        int score = 0;
        score += sugar <= 5 ? 10 : sugar <= 10 ? 5 : sugar >= 20 ? -10 : 0;
        score += fiber >= 5 ? 5 : fiber >= 3 ? 2 : 0;
        return score;
    }

    /**
     * Ưu tiên món ít sodium cho người có cao huyết áp hoặc cần ăn nhạt.
     */
    private int scoreLowSodiumNutrition(MealPlanAiCandidate candidate) {
        double sodium = nutritionValue(candidate.getNutrition().getTotalSodium());
        return sodium <= 400 ? 8 : sodium <= 700 ? 4 : sodium >= 1000 ? -8 : 0;
    }

    /**
     * Ưu tiên món ít chất béo cho người có mỡ máu, cholesterol hoặc vấn đề tim mạch.
     */
    private int scoreLowFatNutrition(MealPlanAiCandidate candidate) {
        double fat = nutritionValue(candidate.getNutrition().getTotalFat());
        return fat <= 15 ? 8 : fat <= 25 ? 4 : fat >= 35 ? -8 : 0;
    }

    /**
     * Kiểm tra một chuỗi có khớp bất kỳ keyword nào sau khi normalize hay không.
     */
    private boolean hasAnyTextMatch(String text, List<String> keywords) {
        return keywords.stream().anyMatch(keyword -> isTextMatch(text, keyword));
    }

    /**
     * Kiểm tra một danh sách chuỗi có phần tử nào khớp keyword hay không.
     */
    private boolean hasAnyTextMatch(List<String> values, List<String> keywords) {
        return values.stream().anyMatch(value -> hasAnyTextMatch(value, keywords));
    }

    /**
     * So khớp một chiều: text phải chứa keyword sau khi normalize.
     */
    private boolean isTextMatch(String text, String keyword) {
        String normalizedText = normalize(text);
        String normalizedKeyword = normalize(keyword);
        return !normalizedText.isBlank()
                && !normalizedKeyword.isBlank()
                && normalizedText.contains(normalizedKeyword);
    }

    /**
     * Chuẩn hóa text về lowercase, bỏ dấu tiếng Việt và trim để match keyword ổn định hơn.
     */
    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        return Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .trim()
                .toLowerCase(Locale.ROOT);
    }

    /**
     * Đổi giá trị dinh dưỡng null thành 0 để công thức score không lỗi null.
     */
    private double nutritionValue(Double value) {
        return value == null ? 0d : value;
    }
}
