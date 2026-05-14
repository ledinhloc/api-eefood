package com.eefood.reactionservice.livestream.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LiveAudioTranscriptionResponse {
  private Long liveStreamId;
  private String fileName;
  private String contentType;
  private String text;
}
