package com.eefood.reactionservice.repository.post;

import com.eefood.reactionservice.model.PostReaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PostReactionRepository extends JpaRepository<PostReaction, Long> {
  Optional<PostReaction> findByPostIdAndUserId(Long postId, Long userId);
  Long countByPostId(Long postId);

  List<PostReaction> findAllByUserIdAndCreatedAtAfter(Long userId, LocalDateTime createdAt);
}
