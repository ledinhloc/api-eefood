package com.eefood.reactionservice.livestream.service;

import com.eefood.reactionservice.dto.response.UserInfo;
import com.eefood.reactionservice.enums.ErrorMessage;
import com.eefood.reactionservice.exception.ExceptionUtil;
import com.eefood.reactionservice.livestream.dto.response.LivePollOptionProposalResponse;
import com.eefood.reactionservice.livestream.enums.PollOptionAddMode;
import com.eefood.reactionservice.livestream.enums.PollOptionProposalStatus;
import com.eefood.reactionservice.livestream.enums.PollStatus;
import com.eefood.reactionservice.livestream.mapper.LivePollMapper;
import com.eefood.reactionservice.livestream.mapper.LivePollOptionProposalMapper;
import com.eefood.reactionservice.livestream.model.LivePoll;
import com.eefood.reactionservice.livestream.model.LivePollOption;
import com.eefood.reactionservice.livestream.model.LivePollOptionProposal;
import com.eefood.reactionservice.livestream.model.LivePollSetting;
import com.eefood.reactionservice.livestream.repository.LivePollOptionProposalRepository;
import com.eefood.reactionservice.livestream.repository.LivePollOptionRepository;
import com.eefood.reactionservice.livestream.repository.LivePollRepository;
import com.eefood.reactionservice.livestream.repository.LivePollSettingRepository;
import com.eefood.reactionservice.repository.httpclient.IamClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LivePollOptionProposalService {

  private final LivePollRepository pollRepo;
  private final LivePollSettingRepository settingRepo;
  private final LivePollOptionRepository optionRepo;
  private final LivePollOptionProposalRepository proposalRepo;
  private final LivePollMapper pollMapper;
  private final LivePollOptionProposalMapper proposalMapper;
  private final LiveStreamService liveStreamService;
  private final LivePollBroadcastService livePollBroadcastService;
  private final IamClient iamClient;

  @Transactional
  public LivePollOptionProposalResponse createProposal(
    Long liveStreamId,
    Long pollId,
    Long userId,
    String req
  ) {
    if ( req == null || req.trim().isBlank()) {
      throw ExceptionUtil.badRequest(ErrorMessage.INVALID_REQUEST);
    }

    LivePoll poll = pollRepo.findByIdAndLiveStreamId(pollId, liveStreamId)
      .orElseThrow(() -> ExceptionUtil.notFound(ErrorMessage.POLL_NOT_FOUND));

    if (poll.getStatus() != PollStatus.OPEN) {
      throw ExceptionUtil.badRequest(ErrorMessage.POLL_NOT_OPEN);
    }

    LivePollSetting setting = settingRepo.findByPollId(pollId)
      .orElseThrow(() -> ExceptionUtil.notFound(ErrorMessage.POLL_SETTING_NOT_FOUND));

    if (setting.getOptionAddMode() != PollOptionAddMode.VIEWER_WITH_APPROVAL) {
      throw ExceptionUtil.badRequest(ErrorMessage.POLL_OPTION_PROPOSAL_NOT_ALLOWED);
    }

    String normalizedText = req.trim();

    if (optionRepo.existsByPollIdAndTextIgnoreCase(pollId, normalizedText)
      || proposalRepo.existsByPollIdAndStatusAndTextIgnoreCase(
        pollId,
        PollOptionProposalStatus.PENDING,
        normalizedText
      )) {
      throw ExceptionUtil.conflict(ErrorMessage.POLL_OPTION_PROPOSAL_DUPLICATED);
    }

    LivePollOptionProposal saved = proposalRepo.save(
      LivePollOptionProposal.builder()
        .pollId(pollId)
        .proposedBy(userId)
        .text(normalizedText)
        .status(PollOptionProposalStatus.PENDING)
        .build()
    );

    LivePollOptionProposalResponse response = proposalMapper.toResponse(saved, fetchUserInfo(userId));
    livePollBroadcastService.broadcastPollProposal(liveStreamId, response);
    return response;
  }

  @Transactional(readOnly = true)
  public List<LivePollOptionProposalResponse> getProposals(
    Long liveStreamId,
    Long pollId,
    Long streamerId,
    PollOptionProposalStatus status
  ) {
    pollRepo.findByIdAndLiveStreamId(pollId, liveStreamId)
      .orElseThrow(() -> ExceptionUtil.notFound(ErrorMessage.POLL_NOT_FOUND));

    if (!liveStreamService.isLiveStreamOwnedByStreamer(liveStreamId, streamerId)) {
      throw ExceptionUtil.forbidden(ErrorMessage.ACCESS_DENIED);
    }

    List<LivePollOptionProposal> proposals = status == null
      ? proposalRepo.findByPollIdOrderByCreatedAtDesc(pollId)
      : proposalRepo.findByPollIdAndStatusOrderByCreatedAtDesc(pollId, status);

    Map<Long, UserInfo> userInfoMap = fetchUserInfoMap(proposals);
    return proposals.stream()
      .map(proposal -> proposalMapper.toResponse(proposal, userInfoMap.get(proposal.getProposedBy())))
      .toList();
  }

  @Transactional
  public LivePollOptionProposalResponse updateProposalStatus(
    Long liveStreamId,
    Long pollId,
    Long proposalId,
    Long streamerId,
    PollOptionProposalStatus status
  ) {
    if (status == null || status == PollOptionProposalStatus.PENDING) {
      throw ExceptionUtil.badRequest(ErrorMessage.INVALID_REQUEST);
    }

    LivePoll poll = pollRepo.findByIdAndLiveStreamId(pollId, liveStreamId)
      .orElseThrow(() -> ExceptionUtil.notFound(ErrorMessage.POLL_NOT_FOUND));

    if (!liveStreamService.isLiveStreamOwnedByStreamer(liveStreamId, streamerId)) {
      throw ExceptionUtil.forbidden(ErrorMessage.ACCESS_DENIED);
    }

    LivePollOptionProposal proposal = proposalRepo.findByIdAndPollId(proposalId, pollId)
      .orElseThrow(() -> ExceptionUtil.notFound(ErrorMessage.POLL_OPTION_PROPOSAL_NOT_FOUND));

    if (proposal.getStatus() != PollOptionProposalStatus.PENDING) {
      throw ExceptionUtil.conflict(ErrorMessage.INVALID_POLL_OPTION_PROPOSAL_STATUS_TRANSITION);
    }

    if (status == PollOptionProposalStatus.APPROVED) {
      if (poll.getStatus() != PollStatus.OPEN) {
        throw ExceptionUtil.badRequest(ErrorMessage.POLL_NOT_OPEN);
      }

      if (optionRepo.existsByPollIdAndTextIgnoreCase(pollId, proposal.getText())) {
        throw ExceptionUtil.conflict(ErrorMessage.POLL_OPTION_PROPOSAL_DUPLICATED);
      }

      optionRepo.save(LivePollOption.builder()
        .pollId(pollId)
        .text(proposal.getText())
        .count(0L)
        .build());

      LivePollSetting setting = settingRepo.findByPollId(pollId)
        .orElseThrow(() -> ExceptionUtil.notFound(ErrorMessage.POLL_SETTING_NOT_FOUND));

      List<LivePollOption> options = optionRepo.findByPollIdOrderByIdAsc(pollId);
      livePollBroadcastService.broadcastPoll(
        liveStreamId,
        pollMapper.toFullResponse(poll, setting, options)
      );
    }

    proposal.setStatus(status);
    proposalRepo.save(proposal);
    return proposalMapper.toResponse(proposal, fetchUserInfo(proposal.getProposedBy()));
  }

  private UserInfo fetchUserInfo(Long userId) {
    var response = iamClient.getUserInfo(userId);
    if (response == null) {
      return null;
    }
    return response.getData();
  }

  private Map<Long, UserInfo> fetchUserInfoMap(List<LivePollOptionProposal> proposals) {
    List<Long> userIds = proposals.stream()
      .map(LivePollOptionProposal::getProposedBy)
      .distinct()
      .toList();

    if (userIds.isEmpty()) {
      return Collections.emptyMap();
    }

    List<UserInfo> userInfos = iamClient.getUserInfoBatch(userIds).getData();
    if (userInfos == null || userInfos.isEmpty()) {
      return Collections.emptyMap();
    }

    return userInfos.stream()
      .collect(Collectors.toMap(UserInfo::getId, user -> user));
  }
}
