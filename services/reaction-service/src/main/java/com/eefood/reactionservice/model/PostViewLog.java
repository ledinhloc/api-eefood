package com.eefood.reactionservice.model;

import com.eefood.reactionservice.model.Post;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "post_view_log")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostViewLog {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "user_id", nullable = false)
  private Long userId;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "post_id", nullable = false)
  private Post post;

  @Column(name = "viewed_at", nullable = false)
  private LocalDateTime viewedAt;

  @Column(name = "view_duration", nullable = false)
  private Long viewDuration;// bang giay
}
