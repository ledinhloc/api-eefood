package com.eefood.reactionservice.livestream.dto.event;

import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LivePollVoteStreamEvent {
  private String eventId;
  private Long liveStreamId;
  private Long pollId;
  private Long userId;
  private List<Long> toAdd;
  private List<Long> toRemove;
  private LocalDateTime occurredAt;
}
