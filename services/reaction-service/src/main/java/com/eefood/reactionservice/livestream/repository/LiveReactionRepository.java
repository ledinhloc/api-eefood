package com.eefood.reactionservice.livestream.repository;

import com.eefood.reactionservice.livestream.model.LiveReaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LiveReactionRepository extends JpaRepository<LiveReaction, Long> {
  List<LiveReaction> findByLiveStreamId(Long liveStreamId);
}