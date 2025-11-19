package com.eefood.reactionservice.repository;

import com.eefood.reactionservice.model.StorySetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StorySettingRepository extends JpaRepository<StorySetting, Long> {
    Optional<StorySetting> findByUserId(Long userId);
}
