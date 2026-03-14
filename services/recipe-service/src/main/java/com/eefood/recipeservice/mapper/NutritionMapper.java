package com.eefood.recipeservice.mapper;

import com.eefood.recipeservice.dto.response.IngredientNutritionDetail;
import com.eefood.recipeservice.dto.response.NutritionAnalysisResponse;
import com.eefood.recipeservice.model.RecipeIngredientNutrition;
import com.eefood.recipeservice.model.RecipeNutrition;
import com.eefood.recipeservice.model.RecipeNutritionAnalysis;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface NutritionMapper {
    @Mapping(target = "ingredientName", source = "ingredient.name")
    IngredientNutritionDetail toDetail(RecipeIngredientNutrition rin);

    @Mapping(target = "recipeId",    source = "nutrition.recipe.id")
    @Mapping(target = "recipeTitle", source = "nutrition.recipe.title")
    @Mapping(target = "totalCalories", source = "nutrition.totalCalories")
    @Mapping(target = "totalProtein",  source = "nutrition.totalProtein")
    @Mapping(target = "totalFat",      source = "nutrition.totalFat")
    @Mapping(target = "totalCarb",     source = "nutrition.totalCarb")
    @Mapping(target = "totalFiber",    source = "nutrition.totalFiber")
    @Mapping(target = "totalSugar",    source = "nutrition.totalSugar")
    @Mapping(target = "totalCalcium",  source = "nutrition.totalCalcium")
    @Mapping(target = "totalSodium",   source = "nutrition.totalSodium")
    @Mapping(target = "healthScore",   source = "nutrition.healthScore")
    @Mapping(target = "summary",        source = "analysis.summary")
    @Mapping(target = "healthLevel",    expression = "java(analysis.getHealthLevel() != null ? analysis.getHealthLevel().name() : null)")
    @Mapping(target = "recommendation", source = "analysis.recommendation")
    @Mapping(target = "ingredientDetails", source = "ingredientDetails")
    NutritionAnalysisResponse toResponse(
            RecipeNutrition nutrition,
            RecipeNutritionAnalysis analysis,
            List<IngredientNutritionDetail> ingredientDetails
    );

    @Mapping(target = "recipeId",          source = "nutrition.recipe.id")
    @Mapping(target = "recipeTitle",       source = "nutrition.recipe.title")
    @Mapping(target = "totalCalories",     source = "nutrition.totalCalories")
    @Mapping(target = "totalProtein",      source = "nutrition.totalProtein")
    @Mapping(target = "totalFat",          source = "nutrition.totalFat")
    @Mapping(target = "totalCarb",         source = "nutrition.totalCarb")
    @Mapping(target = "totalFiber",        source = "nutrition.totalFiber")
    @Mapping(target = "totalSugar",        source = "nutrition.totalSugar")
    @Mapping(target = "totalCalcium",      source = "nutrition.totalCalcium")
    @Mapping(target = "totalSodium",       source = "nutrition.totalSodium")
    @Mapping(target = "healthScore",       source = "nutrition.healthScore")
    @Mapping(target = "summary",           ignore = true)
    @Mapping(target = "healthLevel",       ignore = true)
    @Mapping(target = "recommendation",    ignore = true)
    @Mapping(target = "ingredientDetails", source = "details")
    NutritionAnalysisResponse toPartialResponse(
            RecipeNutrition nutrition,
            List<IngredientNutritionDetail> details
    );
}
