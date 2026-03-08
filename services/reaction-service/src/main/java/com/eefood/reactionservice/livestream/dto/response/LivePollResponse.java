package com.eefood.reactionservice.livestream.dto.response;

import com.eefood.reactionservice.livestream.enums.PollStatus;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LivePollResponse {
  private String id;
  private Long liveStreamId;
  private String question;
  private PollStatus status;
  private LocalDateTime openedAt;
  private LocalDateTime closedAt;

  private LivePollSettingResponse setting;
  private List<LivePollOptionResponse> options;
}
