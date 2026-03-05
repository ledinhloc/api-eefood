package com.eefood.reactionservice.livestream.dto.request;
import com.eefood.reactionservice.livestream.enums.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateLivePollRequest {
  private Long liveStreamId;
  private String question;
  private List<String> options;

  // setting (cho phép null -> service sẽ set default)
  private Boolean allowChangeVote;
  private Boolean multipleChoice;
  private Integer maxChoices;
  private PollResultVisibility resultVisibility;
  private PollVoterVisibility voterVisibility;
  private PollOptionAddMode optionAddMode;
}