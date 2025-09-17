package com.eefood.recipeservice.repository;

import com.eefood.recipeservice.model.Recipe;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RecipeRepository extends JpaRepository<Recipe, Long> {
  @Query("select r from Recipe r where r.isDeleted = false")
  List<Recipe> findAllActive();

  List<Recipe> findByAuthorIdAndIsDeletedFalse(Long authorId);
}
