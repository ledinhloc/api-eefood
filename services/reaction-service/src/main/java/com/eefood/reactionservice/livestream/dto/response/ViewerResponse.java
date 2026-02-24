package com.eefood.reactionservice.livestream.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ViewerResponse {
  private Long userId;
  private String username;
  private String avatarUrl;
  private LocalDateTime joinedAt;
}
