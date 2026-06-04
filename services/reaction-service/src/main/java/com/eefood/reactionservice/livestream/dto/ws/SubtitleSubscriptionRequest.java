package com.eefood.reactionservice.livestream.dto.ws;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubtitleSubscriptionRequest {
  private Long liveStreamId;
  private String targetLanguage;
}
