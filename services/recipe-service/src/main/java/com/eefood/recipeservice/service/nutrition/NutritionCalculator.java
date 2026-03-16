package com.eefood.recipeservice.service.nutrition;

import com.eefood.recipeservice.model.RecipeIngredientNutrition;
import com.eefood.recipeservice.model.RecipeNutrition;
import com.eefood.recipeservice.util.UnitUtils;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Function;

@Component
public class NutritionCalculator {
    public double toGrams(Double quantity, String unit) {
        return UnitUtils.toGrams(quantity, unit);
    }

    public RecipeNutrition calcTotal(List<RecipeIngredientNutrition> rinList) {
        double calories = sum(rinList, RecipeIngredientNutrition::getCalories);
        double protein  = sum(rinList, RecipeIngredientNutrition::getProtein);
        double fat      = sum(rinList, RecipeIngredientNutrition::getFat);
        double carb     = sum(rinList, RecipeIngredientNutrition::getCarb);
        double fiber    = sum(rinList, RecipeIngredientNutrition::getFiber);
        double sugar    = sum(rinList, RecipeIngredientNutrition::getSugar);
        double calcium  = sum(rinList, RecipeIngredientNutrition::getCalcium);
        double sodium   = sum(rinList, RecipeIngredientNutrition::getSodium);

        return RecipeNutrition.builder()
                .totalCalories(round(calories))
                .totalProtein(round(protein))
                .totalFat(round(fat))
                .totalCarb(round(carb))
                .totalFiber(round(fiber))
                .totalSugar(round(sugar))
                .totalCalcium(round(calcium))
                .totalSodium(round(sodium))
                .healthScore(round(calcHealthScore(calories, fat, sodium, protein, fiber, sugar)))
                .build();
    }

    public double calcHealthScore(double calories, double fat, double sodium,
                                  double protein, double fiber, double sugar) {

        double penalty = 0;

        if (calories > 600)
            penalty += Math.min(25, (calories - 600) / 20);

        if (fat > 25)
            penalty += Math.min(20, (fat - 25) / 1.5);

        if (sodium > 800)
            penalty += Math.min(20, (sodium - 800) / 40);

        if (sugar > 15)
            penalty += Math.min(15, (sugar - 15) / 1.5);

        double bonus = 0;

        if (protein >= 20)
            bonus += 8;

        if (fiber >= 5)
            bonus += 7;

        double score = 100 - penalty + bonus;

        return Math.max(0, Math.min(100, score));
    }

    private double sum(List<RecipeIngredientNutrition> list,
                       Function<RecipeIngredientNutrition, Double> getter) {
        return list.stream().mapToDouble(r -> safe(getter.apply(r))).sum();
    }

    public double safe(Double v)        { return v != null ? v : 0.0; }
    public double round(double v)       { return Math.round(v * 100.0) / 100.0; }
}
