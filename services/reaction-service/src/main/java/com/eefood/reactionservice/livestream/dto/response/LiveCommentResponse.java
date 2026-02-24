package com.eefood.reactionservice.livestream.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LiveCommentResponse {
  private Long id;
  private Long userId;
  private String username;
  private String avatarUrl;
  private String message;
  private LocalDateTime createdAt;
}