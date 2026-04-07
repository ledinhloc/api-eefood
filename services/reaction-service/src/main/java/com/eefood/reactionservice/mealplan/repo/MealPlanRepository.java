package com.eefood.reactionservice.mealplan.repo;

import com.eefood.reactionservice.mealplan.model.MealPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MealPlanRepository extends JpaRepository<MealPlan, Long> {
    Optional<MealPlan> findByUserId(Long userId);
}
