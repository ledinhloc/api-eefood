package com.eefood.reactionservice.livestream.model;

import com.eefood.reactionservice.livestream.enums.PollOptionProposalStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "live_poll_option_proposals", indexes = {
  @Index(name = "idx_poll_option_proposals_poll", columnList = "pollId"),
  @Index(name = "idx_poll_option_proposals_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LivePollOptionProposal {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private Long pollId;

  @Column(nullable = false)
  private Long proposedBy;

  @Column(nullable = false, length = 200)
  private String text;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 50)
  @Builder.Default
  private PollOptionProposalStatus status = PollOptionProposalStatus.PENDING;

  @Column(nullable = false)
  private LocalDateTime createdAt;

  @PrePersist
  public void prePersist() {
    createdAt = LocalDateTime.now();
  }
}
