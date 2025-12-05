package com.eefood.reactionservice.dto.response;

import com.eefood.reactionservice.enums.ReactionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostReactionResponse {
  private Long id;
  private Long postId;
  private Long userId;
  private ReactionType reactionType;
  private LocalDateTime createdAt;
}
