package com.eefood.recipeservice.repository;

import com.eefood.recipeservice.enums.CookingStepStatus;
import com.eefood.recipeservice.model.CookingSessionStep;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CookingSessionStepRepository extends JpaRepository<CookingSessionStep, Long> {
    List<CookingSessionStep> findByCookingSessionIdOrderByRecipeStepStepNumberAsc(Long sessionId);
    boolean existsByCookingSessionIdAndStatusNot(Long sessionId, CookingStepStatus status);
    void deleteByCookingSessionId(Long sessionId);
}
