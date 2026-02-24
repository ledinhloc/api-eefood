package com.eefood.reactionservice.livestream.model;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;

import java.time.LocalDateTime;

@Entity
@Table(name = "live_stream_blocks")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LiveStreamBlock {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  //ng bi chan
  @Column(nullable = false)
  private Long blockedUserId;

  //ng chan
  @Column(nullable = false)
  private Long streamerId;

  @CreatedDate
  @Column(nullable = false, updatable = false)
  private LocalDateTime createdAt;
}
