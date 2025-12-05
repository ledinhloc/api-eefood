package com.eefood.reactionservice.repository.story;

import com.eefood.reactionservice.model.StoryCollectionItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StoryCollectionItemRepository extends JpaRepository<StoryCollectionItem, Long> {
    List<StoryCollectionItem> findByCollectionIdOrderByCreatedAtDesc(Long collectionId);
    List<StoryCollectionItem> findByCollectionUserIdAndStoryIdAndCollectionIsDeletedFalseOrderByCreatedAtDesc(Long userId, Long storyId);
    List<StoryCollectionItem> findByCollectionId(Long collectionId);
}
