package com.eefood.recipeservice.repository;

import com.eefood.recipeservice.model.UserIngredientSubstitution;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserIngredientSubstitutionRepository extends JpaRepository<UserIngredientSubstitution, Long> {
    Optional<UserIngredientSubstitution> findByUserIdAndRecipeIngredientId(Long userId, Long recipeIngredientId);

    List<UserIngredientSubstitution> findByUserIdAndRecipeIngredient_Recipe_Id(
            Long userId, Long recipeId);
}
