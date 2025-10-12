package com.eefood.reactionservice.dto.request;

import com.eefood.reactionservice.enums.ReactionType;
import lombok.Data;

@Data
public class PostReactionRequest {
  private Long postId;
  private ReactionType reactionType;
}
