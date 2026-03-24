package com.eefood.reactionservice.mealplan.repo;

import com.eefood.reactionservice.mealplan.model.MealPlanItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MealPlanItemRepository extends JpaRepository<MealPlanItem, Long> {
    List<MealPlanItem> findAllByMealPlanIdOrderByPlanDateAscMealSlotAscItemOrderAsc(Long mealPlanId);
}
