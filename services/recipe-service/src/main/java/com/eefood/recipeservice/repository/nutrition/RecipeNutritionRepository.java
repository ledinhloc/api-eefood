package com.eefood.recipeservice.repository.nutrition;

import com.eefood.recipeservice.model.RecipeNutrition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
@Repository
public interface RecipeNutritionRepository extends JpaRepository<RecipeNutrition, Long> {
    Optional<RecipeNutrition> findByRecipeId(Long recipeId);
}
