package com.eefood.recipeservice.repository;

import com.eefood.recipeservice.model.RecipeIngredient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Set;

public interface RecipeIngredientRepository extends JpaRepository<RecipeIngredient, Long> {
    @Query("SELECT ri FROM RecipeIngredient ri WHERE ri.recipe.id = :recipeId AND ri.isDeleted = false")
    List<RecipeIngredient> findByRecipeIdAndNotDeleted(@Param("recipeId") Long recipeId);
}
