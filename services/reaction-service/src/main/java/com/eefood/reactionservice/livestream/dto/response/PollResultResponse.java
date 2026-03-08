package com.eefood.reactionservice.livestream.dto.response;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PollResultResponse {
  private Long pollId;
  private Long totalVotes;
  private List<LivePollOptionResponse> options;
}