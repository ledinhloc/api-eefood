package com.eefood.reactionservice.repository.livestream;

import com.eefood.reactionservice.model.livestream.LiveReaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LiveReactionRepository extends JpaRepository<LiveReaction, Long> {
  List<LiveReaction> findByLiveStreamId(Long liveStreamId);
}