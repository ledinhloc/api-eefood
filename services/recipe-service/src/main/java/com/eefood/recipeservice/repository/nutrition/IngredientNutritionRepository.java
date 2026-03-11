package com.eefood.recipeservice.repository.nutrition;

import com.eefood.recipeservice.model.IngredientNutrition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface IngredientNutritionRepository extends JpaRepository<IngredientNutrition, Long> {
    Optional<IngredientNutrition> findByIngredientId(Long ingredientId);
}
