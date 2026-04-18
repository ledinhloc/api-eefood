package com.eefood.reactionservice.livestream.repository;

import com.eefood.reactionservice.livestream.model.LivePollVote;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LivePollVoteRepository extends JpaRepository<LivePollVote, Long> {
  List<LivePollVote> findAllByPollIdAndUserId(Long pollId, Long userId);
  List<LivePollVote> findAllByPollIdAndOptionIdOrderByCreatedAtDesc(Long pollId, Long optionId);

  Optional<LivePollVote> findFirstByPollIdAndUserId(Long pollId, Long userId);
  Optional<LivePollVote> findByPollIdAndUserIdAndOptionId(Long pollId, Long userId, Long optionId);

  boolean existsByPollIdAndUserId(Long pollId, Long userId);
  long countByPollIdAndUserId(Long pollId, Long userId);

  void deleteByPollIdAndUserIdAndOptionId(Long pollId, Long userId, Long optionIds);
  long countByPollId(Long pollId);
}
