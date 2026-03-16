package com.eefood.recipeservice.repository;

import com.eefood.recipeservice.model.Recipe;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RecipeRepository extends JpaRepository<Recipe, Long>, JpaSpecificationExecutor<Recipe> {
  Optional<Recipe> findByIdAndIsDeletedFalse(Long id);

  @Query("SELECT r FROM Recipe r LEFT JOIN FETCH r.ingredients ri LEFT JOIN FETCH ri.ingredient WHERE (LOWER(r.title) LIKE LOWER(CONCAT('%', :title, '%')) OR LOWER(r.description) LIKE LOWER(CONCAT('%', :description, '%'))) AND r.isDeleted = false ORDER BY r.id ASC LIMIT 1")
  Optional<Recipe> findFirstByTitleAndDescriptionWithIngredients(@Param("title") String title, @Param("description") String description);

  @Query("SELECT r FROM Recipe r LEFT JOIN FETCH r.ingredients ri LEFT JOIN FETCH ri.ingredient WHERE r.id = :id AND r.isDeleted = false")
  Optional<Recipe> findByIdWithIngredients(@Param("id") Long id);
}
