package com.eefood.reactionservice.dto.response;
import com.eefood.reactionservice.enums.LiveStreamStatus;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LiveStreamResponse {
  private Long id;
  private String roomName;
  private String title;
  private String description;
  private String thumbnailUrl;
  private LiveStreamStatus status;
  private Integer viewerCount;
  private LocalDateTime scheduledAt;
  private LocalDateTime startedAt;
  private LocalDateTime endedAt;

  // Token chỉ trả về khi join/start stream
  private String livekitToken;

  private Long userId;
  private String username;
  private String email;
  private String avatarUrl;
}
