package com.eefood.reactionservice.mealplan.repo;

import com.eefood.reactionservice.mealplan.model.MealPlanItemIngredient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface MealPlanItemIngredientRepository extends JpaRepository<MealPlanItemIngredient, Long> {
    List<MealPlanItemIngredient> findAllByMealPlanItemIdIn(Collection<Long> mealPlanItemIds);
}
