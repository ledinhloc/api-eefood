package com.eefood.reactionservice.repository;

import com.eefood.reactionservice.model.StoryView;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StoryViewRepository extends JpaRepository<StoryView, Long> {
    boolean existsByStoryIdAndUserId(Long storyId, Long userId);
    Page<StoryView> findAllByStoryId(Long storyId, Pageable pageable);
}
