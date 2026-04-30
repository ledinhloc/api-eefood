package com.eefood.recipeservice.repository;

import com.eefood.recipeservice.model.ReviewAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReviewAnswerRepository extends JpaRepository<ReviewAnswer,Long> {
    @Query("""
        SELECT a.question.id, a.option.id, COUNT(a)
        FROM ReviewAnswer a
        WHERE a.review.recipe.id = :recipeId
          AND a.isDeleted = false
          AND a.review.isDeleted = false
        GROUP BY a.question.id, a.option.id
    """)
    List<Object[]> countAnswersByRecipeId(@Param("recipeId") Long recipeId);
}
