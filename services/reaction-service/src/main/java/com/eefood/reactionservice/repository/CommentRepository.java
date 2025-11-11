package com.eefood.reactionservice.repository;

import com.eefood.reactionservice.model.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long>, JpaSpecificationExecutor<Comment> {
  List<Comment> findAllByUserIdAndCreatedAtAfterAndIsDeletedFalse(Long userId, LocalDateTime fromDate);
  List<Comment> findByParentIdAndIsDeletedFalse(Long id);

    Optional<Comment> findByIdAndIsDeletedFalse(Long id);

    Page<Comment> findByPostIdAndParentIsNullAndIsDeletedFalse(Long postId, Pageable pageable);

    Page<Comment> findByParentIdAndIsDeletedFalse(Long parentId, Pageable pageable);

    List<Comment> findByParentIdInAndIsDeletedFalse(List<Long> parentIds);

    @Query("SELECT COUNT(c) FROM Comment c WHERE c.parent.id = :parentId")
    int countByParentId(@Param("parentId") Long parentId);
}
