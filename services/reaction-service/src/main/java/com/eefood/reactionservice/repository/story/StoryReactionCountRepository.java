package com.eefood.reactionservice.repository.story;

import com.eefood.reactionservice.model.Story;
import com.eefood.reactionservice.model.StoryReactionCount;
import com.eefood.reactionservice.model.StoryReactionCountId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StoryReactionCountRepository extends JpaRepository<StoryReactionCount, StoryReactionCountId> {
    List<StoryReactionCount> findAllByStory(Story story);
}
