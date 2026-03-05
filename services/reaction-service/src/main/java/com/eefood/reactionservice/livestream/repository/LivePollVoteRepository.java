package com.eefood.reactionservice.livestream.repository;

import com.eefood.reactionservice.livestream.model.LivePollVote;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LivePollVoteRepository extends JpaRepository<LivePollVote, Long> {
  Optional<LivePollVote> findByPollIdAndUserId(Long  pollId, Long userId);
  long countByPollId(Long pollId);
  void deleteByPollId(Long pollId);
}
