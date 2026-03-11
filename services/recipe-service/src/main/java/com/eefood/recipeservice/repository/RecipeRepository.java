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
  Optional<Recipe> findFirstByTitleContainingIgnoreCaseAndIsDeletedFalse(String title);
}
