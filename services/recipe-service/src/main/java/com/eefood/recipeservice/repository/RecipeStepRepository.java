package com.eefood.recipeservice.repository;

import com.eefood.recipeservice.model.RecipeStep;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RecipeStepRepository extends JpaRepository<RecipeStep, Long> {
  List<RecipeStep> findByRecipeIdAndIsDeletedFalse(Long recipeId);
}