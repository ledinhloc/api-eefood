package com.eefood.reactionservice.model;

import com.eefood.reactionservice.enums.ReactionType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "post_reaction_count")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@IdClass(PostReactionCountId.class)
public class PostReactionCount {
  @Id
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "post_id", nullable = false)
  private Post post;

  @Id
  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private ReactionType reactionType;

  @Column(nullable = false)
  @Builder.Default
  private Long count = 0L;
}
