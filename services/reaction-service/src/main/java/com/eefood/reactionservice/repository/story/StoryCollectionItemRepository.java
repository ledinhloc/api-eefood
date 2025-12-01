package com.eefood.reactionservice.repository.story;

import com.eefood.reactionservice.model.StoryCollectionItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StoryCollectionItemRepository extends JpaRepository<StoryCollectionItem, Long> {
}
