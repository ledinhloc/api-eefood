package com.eefood.reactionservice.repository;

import com.eefood.reactionservice.model.StoryReaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StoryReactionRepository extends CrudRepository<StoryReaction, Long> {
    Optional<StoryReaction> findByStoryIdAndUserId(Long storyId, Long userId);
    Page<StoryReaction> findByStoryId(Long storyId, Pageable pageable);
}
