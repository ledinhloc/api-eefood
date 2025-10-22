package com.eefood.reactionservice.repository;

import com.eefood.reactionservice.model.Comment;
import com.eefood.reactionservice.model.CommentReactionCount;
import com.eefood.reactionservice.model.CommentReactionCountId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentReactionCountRepository extends JpaRepository<CommentReactionCount, CommentReactionCountId> {
    List<CommentReactionCount> findAllByComment(Comment comment);
}
