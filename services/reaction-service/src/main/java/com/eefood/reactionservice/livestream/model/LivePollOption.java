package com.eefood.reactionservice.livestream.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "live_poll_options", indexes = {
  @Index(name = "idx_poll_options_poll", columnList = "pollId")
})
@Getter
@Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class LivePollOption {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private Long pollId;

  @Column(nullable = false, length = 200)
  private String text;

  @Column(nullable = false)
  @Builder.Default
  private Long count = 0L;
}