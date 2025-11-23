package com.eefood.reactionservice.model.livestream;

import com.eefood.reactionservice.model.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@Entity
@Table(name = "live_comment")
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class LiveComment extends BaseEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private Long userId;

  @Column(nullable = false)
  private String message;

  @ManyToOne
  @JoinColumn(name = "live_stream_id", nullable = false)
  private LiveStream liveStream;
}
