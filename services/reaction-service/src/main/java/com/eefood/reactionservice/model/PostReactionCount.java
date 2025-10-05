package com.eefood.reactionservice.model;

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
  @JoinColumn(name = "comment_id", nullable = false)
  private Post post;

  @Id
  @Column(name = "reaction_type")
  private String reactionType;

  @Column(nullable = false)
  @Builder.Default
  private Long count = 0L;
}
