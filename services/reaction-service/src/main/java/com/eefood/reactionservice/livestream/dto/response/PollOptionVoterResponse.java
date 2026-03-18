package com.eefood.reactionservice.livestream.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PollOptionVoterResponse {
  private Long userId;
  private String username;
  private String avatarUrl;
  private LocalDateTime votedAt;
}
