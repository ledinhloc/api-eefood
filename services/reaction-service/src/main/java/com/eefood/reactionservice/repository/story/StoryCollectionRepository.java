package com.eefood.reactionservice.repository.story;

import com.eefood.reactionservice.model.StoryCollection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StoryCollectionRepository extends JpaRepository<StoryCollection, Long> {
}
