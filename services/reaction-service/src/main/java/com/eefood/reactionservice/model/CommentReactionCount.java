package com.eefood.reactionservice.model;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "comment_reaction_count")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@IdClass(CommentReactionCountId.class)
public class CommentReactionCount {

  @Id
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "comment_id", nullable = false)
  private Comment comment;

  @Id
  @Column(name = "reaction_type", nullable = false)
  private String reactionType;

  @Column(nullable = false)
  @Builder.Default
  private Long count = 0L;
}
