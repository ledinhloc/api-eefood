package com.eefood.reactionservice.livestream.mapper;

import com.eefood.reactionservice.livestream.dto.response.LivePollOptionProposalResponse;
import com.eefood.reactionservice.livestream.model.LivePollOptionProposal;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface LivePollOptionProposalMapper {
  LivePollOptionProposalResponse toResponse(LivePollOptionProposal proposal);

  List<LivePollOptionProposalResponse> toResponses(List<LivePollOptionProposal> proposals);
}
