package com.eefood.recipeservice.dto.response;

import lombok.*;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class NutritionAnalysisResponse {
    // Thông tin món ăn
    private Long recipeId;
    private String recipeTitle;

    // Tính tổng các dinh dưỡng
    private Double totalCalories; // kCal
    private Double totalProtein; // g
    private Double totalFat; // g
    private Double totalCarb; // g
    private Double totalFiber; // g
    private Double totalSugar; // g
    private Double totalCalcium; // g
    private Double totalSodium; // g
    // Công thức tính đưa ra điểm
    private Double healthScore;

    // Đánh giá do AI gen
    private String summary;
    private String healthLevel;
    private String recommendation;

    // Danh sách dinh dưỡng từng nguyên liệu
    private List<IngredientNutritionDetail> ingredientDetails;
}
