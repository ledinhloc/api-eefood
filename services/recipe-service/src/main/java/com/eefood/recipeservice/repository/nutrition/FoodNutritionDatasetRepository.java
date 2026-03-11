package com.eefood.recipeservice.repository.nutrition;

import com.eefood.recipeservice.model.FoodNutritionDataset;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FoodNutritionDatasetRepository extends JpaRepository<FoodNutritionDataset, Long> {
    @Query("SELECT f FROM FoodNutritionDataset f WHERE LOWER(f.foodNameVi) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<FoodNutritionDataset> findByFoodNameViContainingIgnoreCase(@Param("keyword") String keyword);
}
