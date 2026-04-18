package com.eefood.reactionservice.livestream.dto.cache;

import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PollUserVoteState {
  private Long pollId;
  private Long userId;
  private Set<Long> optionIds;
}
