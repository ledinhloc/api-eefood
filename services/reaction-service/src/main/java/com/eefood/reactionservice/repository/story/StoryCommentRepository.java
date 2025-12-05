package com.eefood.reactionservice.repository.story;

import com.eefood.reactionservice.model.StoryComment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StoryCommentRepository extends JpaRepository<StoryComment, Long> {
    Optional<StoryComment> findByIdAndIsDeletedFalse(Long id);
    Page<StoryComment> findByStoryIdAndParentCommentIsNullAndIsDeletedFalse(Long storyId, Pageable pageable);
    Page<StoryComment> findByParentCommentIdAndIsDeletedFalse(Long parentId, Pageable pageable);
    List<StoryComment> findByParentCommentIdAndIsDeletedFalse(Long parentId);
}
