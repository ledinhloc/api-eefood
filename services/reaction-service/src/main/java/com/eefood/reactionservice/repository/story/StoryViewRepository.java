package com.eefood.reactionservice.repository.story;

import com.eefood.reactionservice.model.StoryView;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StoryViewRepository extends JpaRepository<StoryView, Long> {
    boolean existsByStoryIdAndUserId(Long storyId, Long userId);
    Page<StoryView> findAllByStoryId(Long storyId, Pageable pageable);

    Optional<StoryView> findByIdAndUserId(Long id, Long userId);
}
