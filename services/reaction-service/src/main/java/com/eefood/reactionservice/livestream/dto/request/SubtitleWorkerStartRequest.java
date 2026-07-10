package com.eefood.reactionservice.livestream.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubtitleWorkerStartRequest {
  private Long liveStreamId;
  private String roomName;
  private String spokenLanguage;
  private String targetLanguage;
}
