package com.eefood.recipeservice.repository.nutrition;

import com.eefood.recipeservice.model.IngredientNutrition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface IngredientNutritionRepository extends JpaRepository<IngredientNutrition, Long> {
    Optional<IngredientNutrition> findByIngredientId(Long ingredientId);
    @Query("SELECT n FROM IngredientNutrition n WHERE n.ingredient.id IN :ids")
    List<IngredientNutrition> findByIngredientIdIn(@Param("ids") List<Long> ids);
}
