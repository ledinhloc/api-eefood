package com.eefood.recipeservice.repository;

import com.eefood.recipeservice.enums.CookingSessionStatus;
import com.eefood.recipeservice.model.CookingSessions;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CookingSessionsRepository extends JpaRepository<CookingSessions, Long> {
    Optional<CookingSessions> findByUserIdAndRecipeIdAndStatus(Long userId, Long recipeId, CookingSessionStatus status);
}
