package com.eefood.reactionservice.livestream.repository;

import com.eefood.reactionservice.livestream.model.LivePollOption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface LivePollOptionRepository extends JpaRepository<LivePollOption, Long> {
  List<LivePollOption> findByPollIdOrderByIdAsc(Long livePollId);
  Optional<LivePollOption> findByIdAndPollId(Long id, Long pollId);

  @Modifying(flushAutomatically = true, clearAutomatically = true)
  @Query("update LivePollOption o set o.count = o.count + :delta where o.id = :optionId")
  int addCount(@Param("optionId") Long optionId, @Param("delta") long delta);

  List<LivePollOption> findAllByPollIdAndIdIn(Long pollId, List<Long> ids);
}
