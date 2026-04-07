package com.eefood.recipeservice.repository;

import com.eefood.recipeservice.model.Ingredient;
import com.eefood.recipeservice.model.IngredientSubstitute;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IngredientSubstituteRepository extends JpaRepository<IngredientSubstitute, Long> {
    @Query("SELECT isub.substitute FROM IngredientSubstitute isub WHERE isub.ingredient.id = :ingredientId")
    List<Ingredient> findSubstitutesByIngredientId(@Param("ingredientId") Long ingredientId);
}
