package com.eefood.reactionservice.livestream.repository;

import com.eefood.reactionservice.livestream.enums.PollOptionProposalStatus;
import com.eefood.reactionservice.livestream.model.LivePollOptionProposal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LivePollOptionProposalRepository extends JpaRepository<LivePollOptionProposal, Long> {
  java.util.Optional<LivePollOptionProposal> findByIdAndPollId(Long id, Long pollId);

  List<LivePollOptionProposal> findByPollIdOrderByCreatedAtDesc(Long pollId);

  List<LivePollOptionProposal> findByPollIdAndStatusOrderByCreatedAtDesc(
    Long pollId,
    PollOptionProposalStatus status
  );

  boolean existsByPollIdAndStatusAndTextIgnoreCase(
    Long pollId,
    PollOptionProposalStatus status,
    String text
  );
}
