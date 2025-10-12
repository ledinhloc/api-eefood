package com.eefood.reactionservice.repository;

import com.eefood.reactionservice.model.PostReaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PostReactionRepository extends JpaRepository<PostReaction, Long> {
  Optional<PostReaction> findByPostIdAndUserId(Long postId, Long userId);
  Long countByPostId(Long postId);
}
