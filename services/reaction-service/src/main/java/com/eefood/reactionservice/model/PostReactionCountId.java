package com.eefood.reactionservice.model;

import com.eefood.reactionservice.enums.ReactionType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PostReactionCountId implements Serializable {
  private Long post;
  private ReactionType reactionType;
}