package com.eefood.reactionservice.repository.story;

import com.eefood.reactionservice.model.StoryCollection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StoryCollectionRepository extends JpaRepository<StoryCollection, Long> {
    Page<StoryCollection> findByUserIdAndIsDeletedFalse(Long userId, Pageable pageable);
    List<StoryCollection> findByUserIdAndIsDeletedFalse(Long userId);
}
