package com.eefood.reactionservice.livestream.dto.ws;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LiveSubtitleMessage {
  private Long liveStreamId;
  private String targetLanguage;
  private String text;
  private LocalDateTime createdAt;
}
