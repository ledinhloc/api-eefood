package com.eefood.reactionservice.repository.post;

import com.eefood.reactionservice.model.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PostRepository extends JpaRepository<Post, Long>, JpaSpecificationExecutor<Post> {

  Post findByIdAndIsDeletedFalse(Long id);

  Page<Post> findByIdInAndIsDeletedFalse(List<Long> ids, Pageable pageable);
  boolean existsByRecipeIdAndIsDeletedFalse(Long recipeId);

  List<Post> findAllByUserIdAndIsDeletedFalseOrderByCreatedAtDesc(Long userId);

  Post findByRecipeIdAndIsDeletedFalse(long id);
}