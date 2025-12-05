package com.eefood.reactionservice.repository.story;

import com.eefood.reactionservice.model.Story;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StoryRepository extends JpaRepository<Story, Long> {
    Optional<Story> findByIdAndIsDeletedFalse(Long id);
    List<Story> findAllByUserIdAndIsDeletedFalse(Long userId);
    List<Story> findAllByIsDeletedFalse();
}
