package com.eefood.reactionservice.livestream.model;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "live_view")
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class LiveView{
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private Long userId;

  // thời điểm user join phòng
  @Column(nullable = false)
  private LocalDateTime joinedAt;

  // thời điểm user rời phòng (null nếu đang xem)
  private LocalDateTime leftAt;

  @Column(nullable = false)
  private Long liveStreamId;
}
