package com.eefood.reactionservice.livestream.mapper;

import com.eefood.reactionservice.dto.response.UserInfo;
import com.eefood.reactionservice.livestream.dto.response.LivePollOptionProposalResponse;
import com.eefood.reactionservice.livestream.model.LivePollOptionProposal;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface LivePollOptionProposalMapper {
  @Mapping(target = "id", source = "proposal.id")
  @Mapping(target = "pollId", source = "proposal.pollId")
  @Mapping(target = "proposedBy", source = "proposal.proposedBy")
  @Mapping(target = "text", source = "proposal.text")
  @Mapping(target = "status", source = "proposal.status")
  @Mapping(target = "createdAt", source = "proposal.createdAt")
  @Mapping(target = "username", source = "userInfo.username")
  @Mapping(target = "email", source = "userInfo.email")
  @Mapping(target = "avatarUrl", source = "userInfo.avatarUrl")
  LivePollOptionProposalResponse toResponse(LivePollOptionProposal proposal, UserInfo userInfo);

  List<LivePollOptionProposalResponse> toResponses(List<LivePollOptionProposal> proposals);
}
