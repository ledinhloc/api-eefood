package com.eefood.reactionservice.livestream.repository;

import com.eefood.reactionservice.livestream.model.LiveStreamBlock;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LiveStreamBlockRepository extends JpaRepository<LiveStreamBlock, Long> {
  boolean existsByStreamerIdAndBlockedUserId(Long streamerId, Long blockedUserId);
  List<LiveStreamBlock> findAllByStreamerIdOrderByCreatedAtDesc(Long streamerId);
  void deleteByStreamerIdAndBlockedUserId(Long streamerId, Long blockedUserId);
}
