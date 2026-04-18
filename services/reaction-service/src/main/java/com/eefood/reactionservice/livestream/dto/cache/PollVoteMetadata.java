package com.eefood.reactionservice.livestream.dto.cache;

import com.eefood.reactionservice.livestream.enums.PollStatus;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PollVoteMetadata {
  private Long pollId;
  private Long liveStreamId;
  private PollStatus status;
  private Boolean multipleChoice;
  private Boolean allowChangeVote;
  private Integer maxChoices;
  private Set<Long> optionIds;
}
