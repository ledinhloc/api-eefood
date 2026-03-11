package com.eefood.recipeservice.repository.nutrition;

import com.eefood.recipeservice.model.RecipeIngredientNutrition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RecipeIngredientNutritionRepository extends JpaRepository<RecipeIngredientNutrition, Long> {
    void deleteByRecipeId(Long recipeId);
    List<RecipeIngredientNutrition> findByRecipeId(Long recipeId);
}
