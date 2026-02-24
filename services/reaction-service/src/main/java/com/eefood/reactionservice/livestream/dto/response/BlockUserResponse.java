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
public class BlockUserResponse {
  private Long blockedUserId;
  private String username;
  private String avatarUrl;
  private String email;
  private LocalDateTime createdAt;
}
