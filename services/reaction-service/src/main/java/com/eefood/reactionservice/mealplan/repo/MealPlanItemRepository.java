package com.eefood.reactionservice.mealplan.repo;

import com.eefood.reactionservice.mealplan.model.MealPlanItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface MealPlanItemRepository extends JpaRepository<MealPlanItem, Long> {
    List<MealPlanItem> findAllByMealPlanIdOrderByPlanDateAscMealSlotAscItemOrderAsc(Long mealPlanId);

    void deleteAllByMealPlanId(Long mealPlanId);

    void deleteAllByMealPlanIdAndPlanDateBetween(Long mealPlanId, LocalDate startDate, LocalDate endDate);

    java.util.Optional<MealPlanItem> findByIdAndMealPlanId(Long id, Long mealPlanId);
}
