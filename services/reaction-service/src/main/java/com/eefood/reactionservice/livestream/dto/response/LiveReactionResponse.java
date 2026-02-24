package com.eefood.reactionservice.livestream.dto.response;

import com.eefood.reactionservice.enums.FoodEmotion;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LiveReactionResponse {
  private Long id;

  private Long liveStreamId;
  private FoodEmotion emotion;

  private Long userId;
  private String username;
  private String avatarUrl;
  private LocalDateTime createdAt;
}
