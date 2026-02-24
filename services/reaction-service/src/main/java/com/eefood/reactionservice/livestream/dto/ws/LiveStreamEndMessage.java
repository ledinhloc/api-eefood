package com.eefood.reactionservice.livestream.dto.ws;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LiveStreamEndMessage {
  private String type; // "STREAM_ENDED"
  private Long liveStreamId;
  private String message;
  private LocalDateTime endedAt;
}