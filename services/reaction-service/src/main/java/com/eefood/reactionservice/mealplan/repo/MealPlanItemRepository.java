package com.eefood.reactionservice.mealplan.repo;

import com.eefood.reactionservice.mealplan.enums.MealSlot;
import com.eefood.reactionservice.mealplan.enums.MealPlanItemStatus;
import com.eefood.reactionservice.mealplan.model.MealPlanItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface MealPlanItemRepository extends JpaRepository<MealPlanItem, Long> {
    List<MealPlanItem> findAllByMealPlanIdOrderByPlanDateAscMealSlotAscItemOrderAsc(Long mealPlanId);

    List<MealPlanItem> findAllByMealPlanIdAndPlanDate(Long mealPlanId, LocalDate planDate);

    List<MealPlanItem> findAllByPlanDateAndMealSlotAndStatus(LocalDate planDate, MealSlot mealSlot, MealPlanItemStatus status);

    void deleteAllByMealPlanId(Long mealPlanId);

    void deleteAllByMealPlanIdAndPlanDateBetween(Long mealPlanId, LocalDate startDate, LocalDate endDate);

    Optional<MealPlanItem> findByIdAndMealPlanId(Long id, Long mealPlanId);

    Optional<MealPlanItem> findFirstByMealPlanIdAndPlanDateAndMealSlotAndRecipeId(
            Long mealPlanId,
            LocalDate planDate,
            MealSlot mealSlot,
            Long recipeId
    );
}
