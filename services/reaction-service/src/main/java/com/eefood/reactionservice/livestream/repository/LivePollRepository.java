package com.eefood.reactionservice.livestream.repository;

import com.eefood.reactionservice.livestream.enums.PollStatus;
import com.eefood.reactionservice.livestream.model.LivePoll;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LivePollRepository extends JpaRepository<LivePoll, Long> {
  Optional<LivePoll> findByIdAndLiveStreamId(Long id, Long liveStreamId);
  Optional<LivePoll> findFirstByLiveStreamIdAndStatusOrderByOpenedAtDescIdDesc(
    Long liveStreamId,
    PollStatus status
  );
}
