package com.eefood.recipeservice.repository;

import com.eefood.recipeservice.model.RecipeReview;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RecipeReviewRepository extends JpaRepository<RecipeReview, Long> {
    Optional<RecipeReview> findByUserIdAndRecipeIdAndIsDeletedIsFalse(Long userId, Long recipeId);

    // Đếm tổng review và avg rating
    @Query("""
    SELECT COUNT(r), AVG(r.rating)
    FROM RecipeReview r
    WHERE r.recipe.id = :recipeId AND r.isDeleted = false""")
    List<Object[]> getCountAndAvgRating(@Param("recipeId") Long recipeId);

    // Phân bổ rating theo từng mức (làm tròn để group)
    @Query("""
        SELECT FLOOR(r.rating), COUNT(r)
        FROM RecipeReview r
        WHERE r.recipe.id = :recipeId AND r.isDeleted = false
        GROUP BY FLOOR(r.rating)
        ORDER BY FLOOR(r.rating) DESC
    """)
    List<Object[]> getRatingDistribution(@Param("recipeId") Long recipeId);

    // Top 5 reviews mới nhất
    @Query("""
        SELECT r FROM RecipeReview r
        WHERE r.recipe.id = :recipeId AND r.isDeleted = false
        ORDER BY r.createdAt DESC
    """)
    List<RecipeReview> findTop5ByRecipeId(@Param("recipeId") Long recipeId, Pageable pageable);

    // Phân trang
    @Query("""
        SELECT r FROM RecipeReview r
        WHERE r.recipe.id = :recipeId AND r.isDeleted = false
        ORDER BY r.createdAt DESC
    """)
    Page<RecipeReview> findAllByRecipeId(@Param("recipeId") Long recipeId, Pageable pageable);
}
