package com.eefood.recipeservice.repository;

import com.eefood.recipeservice.model.RecipeReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RecipeReviewRepository extends JpaRepository<RecipeReview, Long> {
    Optional<RecipeReview> findByUserIdAndRecipeIdAndIsDeletedIsFalse(Long userId, Long recipeId);
}
