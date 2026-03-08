package com.eefood.reactionservice.livestream.model;
import com.eefood.reactionservice.livestream.enums.PollStatus;
import com.eefood.reactionservice.model.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "live_polls", indexes = {
  @Index(name = "idx_live_polls_livestream", columnList = "liveStreamId")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class LivePoll {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private Long liveStreamId;

  @Column(nullable = false, length = 500)
  private String question;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  @Builder.Default
  private PollStatus status = PollStatus.DRAFT;

  private LocalDateTime openedAt;
  private LocalDateTime closedAt;
}