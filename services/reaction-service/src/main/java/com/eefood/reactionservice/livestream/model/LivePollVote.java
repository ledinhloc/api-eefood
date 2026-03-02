package com.eefood.reactionservice.livestream.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

// LivePollVote.java
@Entity
@Table(name = "live_poll_votes",
  uniqueConstraints = @UniqueConstraint(name = "uk_poll_user", columnNames = {"pollId","userId"}),
  indexes = {
    @Index(name = "idx_votes_poll", columnList = "pollId")
  }
)
@Getter
@Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class LivePollVote {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private Long pollId;

  @Column(nullable = false)
  private Long userId;

  @Column(nullable = false)
  private Long optionId;

  @Column(nullable = false)
  private LocalDateTime createdAt;
}