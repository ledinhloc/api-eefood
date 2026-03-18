package com.eefood.reactionservice.livestream.dto.response;

import com.eefood.reactionservice.livestream.enums.PollOptionProposalStatus;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LivePollOptionProposalResponse {
  private Long id;
  private Long pollId;
  private Long proposedBy;
  private String username;
  private String email;
  private String avatarUrl;
  private String text;
  private PollOptionProposalStatus status;
  private LocalDateTime createdAt;
}
