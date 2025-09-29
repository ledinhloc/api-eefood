package com.eefood.recipeservice.repository;

import com.eefood.recipeservice.model.RecipeIngredient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Set;

public interface RecipeIngredientRepository extends JpaRepository<RecipeIngredient, Long> {
    @Query("SELECT ri FROM RecipeIngredient ri WHERE ri.recipe.id = :recipeId AND ri.isDeleted = false")
    Set<RecipeIngredient> findByRecipeIdAndIsDeletedFalse(@Param("recipeId") Long recipeId);
}
