package com.eefood.reactionservice.livestream.repository;

import com.eefood.reactionservice.livestream.model.LiveView;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface LiveViewRepository extends JpaRepository<LiveView, Long> {
  @Query("""
    SELECT lv FROM LiveView lv
    WHERE lv.liveStreamId = :liveStreamId 
    AND lv.leftAt IS NULL
    ORDER BY lv.joinedAt DESC
  """)
  List<LiveView> findActiveViewers(Long liveStreamId);

  Optional<LiveView> findByLiveStreamIdAndUserIdAndLeftAtIsNull(Long liveStreamId, Long userId);

  boolean existsByLiveStreamIdAndUserIdAndLeftAtIsNull(Long liveStreamId, Long userId);
}
