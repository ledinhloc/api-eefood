package com.eefood.reactionservice.livestream.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LiveSubtitleTranscriptRequest {
  private Long liveStreamId;
  private String spokenLanguage;
  private String targetLanguage;
  private String text;
  private LocalDateTime createdAt;
}
