package com.eefood.reactionservice.livestream.dto.response;
import com.eefood.reactionservice.livestream.enums.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LivePollSettingResponse {
  private Boolean allowChangeVote;
  private Boolean multipleChoice;
  private Integer maxChoices;
  private PollResultVisibility resultVisibility;
  private PollVoterVisibility voterVisibility;
  private PollOptionAddMode optionAddMode;
}