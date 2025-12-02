package com.eefood.reactionservice.repository.comment;

import com.eefood.reactionservice.model.CommentReaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CommentReactionRepository extends JpaRepository<CommentReaction, Long> {
    Optional<CommentReaction> findByCommentIdAndUserId(Long commentId, Long userId);
    Page<CommentReaction> findByCommentId(Long commentId, Pageable pageable);
    List<CommentReaction> findAllByCommentIdAndUserId(Long commentId, Long userId);
}
