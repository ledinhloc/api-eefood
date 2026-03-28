package com.eefood.reactionservice.repository.post;

import com.eefood.reactionservice.enums.PostStatus;
import com.eefood.reactionservice.model.Post;
import jakarta.persistence.QueryHint;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PostRepository extends JpaRepository<Post, Long>, JpaSpecificationExecutor<Post> {

  Post findByIdAndIsDeletedFalse(Long id);

  Post findByIdAndStatusAndIsDeletedFalse(Long id, PostStatus status);

  Page<Post> findByIdInAndIsDeletedFalse(List<Long> ids, Pageable pageable);
  boolean existsByRecipeIdAndIsDeletedFalse(Long recipeId);

  List<Post> findAllByUserIdAndIsDeletedFalseOrderByCreatedAtDesc(Long userId);

  Post findByRecipeIdAndIsDeletedFalse(long id);

  Post findByRecipeIdAndStatusAndIsDeletedFalse(long id, PostStatus status);

    @Query("""
      SELECT p.userId, COUNT(p.id)
      FROM Post p
      WHERE p.isDeleted = false
      GROUP BY p.userId
      ORDER BY COUNT(p.id) DESC
    """)
  List<Object[]> findTopUsersByPostCount(Pageable pageable);

  Long countByStatus(PostStatus status);


  List<Post> findByStatusAndIsDeletedFalse(PostStatus status, Pageable pageable);

  @Query("""
   SELECT DISTINCT p
   FROM Post p
   LEFT JOIN FETCH p.reactions
   LEFT JOIN FETCH p.recipeCategories
   LEFT JOIN FETCH p.recipeIngredientKeywords
   WHERE p.id IN :ids""")
  List<Post> findAllById(@Param("ids") Iterable<Long> ids);
}
