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
    @Query(value = """
        SELECT DISTINCT * FROM food_nutrition_dataset f
        WHERE EXISTS (
            SELECT 1 FROM unnest(string_to_array(:keywords, ',')) k
            WHERE LOWER(f.food_name_vi) LIKE CONCAT('%', TRIM(k), '%')
               OR LOWER(f.food_name_vi) = TRIM(k)
        )
        """, nativeQuery = true)
    List<FoodNutritionDataset> findByFoodNameViContainingAnyKeyword(
            @Param("keywords") String keywords);
}
