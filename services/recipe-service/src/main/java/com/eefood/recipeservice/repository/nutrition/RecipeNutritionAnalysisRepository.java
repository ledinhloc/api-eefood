package com.eefood.recipeservice.repository.nutrition;

import com.eefood.recipeservice.model.RecipeNutritionAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RecipeNutritionAnalysisRepository extends JpaRepository<RecipeNutritionAnalysis, Long> {
    Optional<RecipeNutritionAnalysis> findByRecipeId(Long recipeId);
}
