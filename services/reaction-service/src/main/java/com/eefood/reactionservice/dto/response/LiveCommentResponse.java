package com.eefood.reactionservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
  private String createdAt;
}