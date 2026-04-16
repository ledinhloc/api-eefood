package com.eefood.reactionservice.livestream.service;


import com.eefood.reactionservice.dto.response.UserInfo;
import com.eefood.reactionservice.enums.ErrorMessage;
import com.eefood.reactionservice.exception.ExceptionUtil;
import com.eefood.reactionservice.livestream.dto.cache.PollVoteMetadata;
import com.eefood.reactionservice.livestream.dto.request.CreateLivePollRequest;
import com.eefood.reactionservice.livestream.dto.response.LivePollResponse;
import com.eefood.reactionservice.livestream.dto.response.PollOptionVoterResponse;
import com.eefood.reactionservice.livestream.dto.response.PollOptionVotersResponse;
import com.eefood.reactionservice.livestream.dto.response.PollResultResponse;
import com.eefood.reactionservice.livestream.enums.PollStatus;
import com.eefood.reactionservice.livestream.mapper.LivePollMapper;
import com.eefood.reactionservice.livestream.model.*;
import com.eefood.reactionservice.livestream.repository.LivePollOptionRepository;
import com.eefood.reactionservice.livestream.repository.LivePollRepository;
import com.eefood.reactionservice.livestream.repository.LivePollSettingRepository;
import com.eefood.reactionservice.livestream.repository.LivePollVoteRepository;
import com.eefood.reactionservice.repository.httpclient.IamClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LivePollService {

  private final LivePollRepository pollRepo;
  private final LivePollOptionRepository optionRepo;
  private final LivePollSettingRepository settingRepo;
  private final LivePollVoteRepository voteRepo;
  private final LivePollMetadataCacheService livePollMetadataCacheService;

  private final LivePollMapper pollMapper;
  private final LivePollBroadcastService livePollBroadcastService;
  private final IamClient iamClient;

  @Transactional(readOnly = true)
  public LivePollResponse getActivePoll(Long liveStreamId) {
    LivePoll poll = pollRepo
      .findFirstByLiveStreamIdAndStatusOrderByOpenedAtDescIdDesc(liveStreamId, PollStatus.OPEN)
      .orElse(null);

    if (poll == null) {
      return null;
    }

    LivePollSetting setting = settingRepo.findByPollId(poll.getId())
      .orElseThrow(() -> ExceptionUtil.notFound(ErrorMessage.POLL_SETTING_NOT_FOUND));

    List<LivePollOption> options = optionRepo.findByPollIdOrderByIdAsc(poll.getId());

    return pollMapper.toFullResponse(poll, setting, options);
  }

  @Transactional
  public LivePollResponse updateStatus(Long liveStreamId, Long pollId, Long userId, PollStatus newStatus) {
    if (newStatus == null) {
      throw ExceptionUtil.badRequest(ErrorMessage.INVALID_REQUEST);
    }

    LivePoll poll = pollRepo.findByIdAndLiveStreamId(pollId, liveStreamId)
      .orElseThrow(() -> ExceptionUtil.notFound(ErrorMessage.POLL_NOT_FOUND));

    PollStatus current = poll.getStatus();

    if (current == newStatus) {
      LivePollSetting setting = settingRepo.findByPollId(pollId)
        .orElseThrow(() -> ExceptionUtil.notFound(ErrorMessage.POLL_SETTING_NOT_FOUND));
      List<LivePollOption> options = optionRepo.findByPollIdOrderByIdAsc(pollId);
      return pollMapper.toFullResponse(poll, setting, options);
    }

    if (current == PollStatus.DRAFT && newStatus == PollStatus.OPEN) {
      poll.setStatus(PollStatus.OPEN);
      poll.setOpenedAt(LocalDateTime.now());
    } else if (current == PollStatus.OPEN && newStatus == PollStatus.CLOSED) {
      poll.setStatus(PollStatus.CLOSED);
      poll.setClosedAt(LocalDateTime.now());
    } else {
      throw ExceptionUtil.badRequest(ErrorMessage.INVALID_POLL_STATUS_TRANSITION);
    }

    pollRepo.save(poll);
    livePollMetadataCacheService.evictPollVoteMetadata(pollId);

    LivePollSetting setting = settingRepo.findByPollId(pollId)
      .orElseThrow(() -> ExceptionUtil.notFound(ErrorMessage.POLL_SETTING_NOT_FOUND));
    List<LivePollOption> options = optionRepo.findByPollIdOrderByIdAsc(pollId);

    LivePollResponse response = pollMapper.toFullResponse(poll, setting, options);
    livePollBroadcastService.broadcastPoll(liveStreamId, response);

    PollResultResponse result = buildResult(pollId);
    livePollBroadcastService.broadcastPollResult(liveStreamId, result);
    return response;
  }

  @Transactional
  public LivePollResponse create(CreateLivePollRequest req, Long liveStreamId) {
    if (req == null) throw ExceptionUtil.badRequest(ErrorMessage.INVALID_REQUEST);
    if (liveStreamId == null) throw ExceptionUtil.badRequest(ErrorMessage.INVALID_LIVESTREAM_ID);
    if (req.getQuestion() == null || req.getQuestion().trim().isBlank()) {
      throw ExceptionUtil.badRequest(ErrorMessage.INVALID_POLL_QUESTION);
    }
    if (req.getOptions() == null || req.getOptions().size() < 2) {
      throw ExceptionUtil.badRequest(ErrorMessage.INVALID_POLL_OPTIONS);
    }

    LivePoll poll = pollRepo.save(LivePoll.builder()
      .liveStreamId(liveStreamId)
      .question(req.getQuestion().trim())
      .status(PollStatus.DRAFT)
      .build());

    for (String opt : req.getOptions()) {
      if (opt == null || opt.trim().isBlank()) {
        throw ExceptionUtil.badRequest(ErrorMessage.INVALID_POLL_OPTIONS);
      }
      optionRepo.save(LivePollOption.builder()
        .pollId(poll.getId())
        .text(opt.trim())
        .count(0L)
        .build());
    }

    // setting
    LivePollSetting setting = LivePollSetting.builder()
      .pollId(poll.getId())
      .allowChangeVote(req.getAllowChangeVote() != null ? req.getAllowChangeVote() : false)
      .multipleChoice(req.getMultipleChoice() != null ? req.getMultipleChoice() : false)
      .maxChoices(req.getMaxChoices() != null ? req.getMaxChoices() : 1)
      .resultVisibility(req.getResultVisibility() != null ? req.getResultVisibility() : settingDefault().getResultVisibility())
      .voterVisibility(req.getVoterVisibility() != null ? req.getVoterVisibility() : settingDefault().getVoterVisibility())
      .optionAddMode(req.getOptionAddMode() != null ? req.getOptionAddMode() : settingDefault().getOptionAddMode())
      .build();

    normalizeSetting(setting);
    settingRepo.save(setting);

    List<LivePollOption> options = optionRepo.findByPollIdOrderByIdAsc(poll.getId());
    return pollMapper.toFullResponse(poll, setting, options);
  }

  @Transactional(readOnly = true)
  public LivePollResponse detail(Long liveStreamId, Long pollId) {
    LivePoll poll = pollRepo.findByIdAndLiveStreamId(pollId, liveStreamId)
      .orElseThrow(() -> ExceptionUtil.notFound(ErrorMessage.POLL_NOT_FOUND));

    LivePollSetting setting = settingRepo.findByPollId(pollId)
      .orElseThrow(() -> ExceptionUtil.notFound(ErrorMessage.POLL_SETTING_NOT_FOUND));

    List<LivePollOption> options = optionRepo.findByPollIdOrderByIdAsc(pollId);
    return pollMapper.toFullResponse(poll, setting, options);
  }

  @Transactional
  public PollResultResponse vote(Long liveStreamId, Long pollId, Long userId, List<Long> optionIds) {

    if (optionIds == null || optionIds.isEmpty()) {
      throw ExceptionUtil.badRequest(ErrorMessage.INVALID_REQUEST);
    }

    optionIds = optionIds.stream()
      .filter(Objects::nonNull)
      .distinct()
      .toList();

    PollVoteMetadata pollMetadata = livePollMetadataCacheService.getPollVoteMetadata(pollId);

    if (!Objects.equals(pollMetadata.getLiveStreamId(), liveStreamId)) {
      throw ExceptionUtil.notFound(ErrorMessage.POLL_NOT_FOUND);
    }

    if (pollMetadata.getStatus() != PollStatus.OPEN) {
      throw ExceptionUtil.badRequest(ErrorMessage.POLL_NOT_OPEN);
    }

    Set<Long> validOptionIds = pollMetadata.getOptionIds();
    if (validOptionIds == null || !validOptionIds.containsAll(optionIds)) {
      throw ExceptionUtil.badRequest(ErrorMessage.POLL_OPTION_INVALID);
    }

    // =========================
    // SINGLE CHOICE
    // =========================
    if (!Boolean.TRUE.equals(pollMetadata.getMultipleChoice())) {

      if (optionIds.size() != 1) {
        throw ExceptionUtil.badRequest(ErrorMessage.POLL_SINGLE_CHOICE_ONLY);
      }

      Long optionId = optionIds.get(0);

      List<LivePollVote> existingVotes = voteRepo.findAllByPollIdAndUserId(pollId, userId);
      LivePollVote existing = existingVotes.isEmpty() ? null : existingVotes.get(0);

      if (existing == null) {

        voteRepo.save(LivePollVote.builder()
          .pollId(pollId)
          .userId(userId)
          .optionId(optionId)
          .createdAt(LocalDateTime.now())
          .build());

        optionRepo.addCount(optionId, 1);

        PollResultResponse result = buildResult(pollId);
        livePollBroadcastService.broadcastPollResult(liveStreamId, result);
        return result;
      }

      if (!Boolean.TRUE.equals(pollMetadata.getAllowChangeVote())) {
        throw ExceptionUtil.conflict(ErrorMessage.POLL_ALREADY_VOTED);
      }

      if (existing.getOptionId().equals(optionId)) {
        return buildResult(pollId);
      }

      optionRepo.addCount(existing.getOptionId(), -1);
      optionRepo.addCount(optionId, 1);

      existing.setOptionId(optionId);
      voteRepo.save(existing);

      PollResultResponse result = buildResult(pollId);
      livePollBroadcastService.broadcastPollResult(liveStreamId, result);
      return result;
    }

    // =========================
    // MULTIPLE CHOICE
    // =========================

    int maxChoices = pollMetadata.getMaxChoices() != null ? pollMetadata.getMaxChoices() : 1;

    if (optionIds.size() > maxChoices) {
      throw ExceptionUtil.conflict(ErrorMessage.POLL_MAX_CHOICES_EXCEEDED);
    }

    List<LivePollVote> existingVotes = voteRepo.findAllByPollIdAndUserId(pollId, userId);
    Set<Long> oldOptionIds = existingVotes.stream()
      .map(LivePollVote::getOptionId)
      .collect(Collectors.toSet());

    Set<Long> newOptionIds = new HashSet<>(optionIds);

    if (!oldOptionIds.isEmpty() && !Boolean.TRUE.equals(pollMetadata.getAllowChangeVote())) {
      throw ExceptionUtil.conflict(ErrorMessage.POLL_ALREADY_VOTED);
    }

    Set<Long> toAdd = new HashSet<>(newOptionIds);
    toAdd.removeAll(oldOptionIds);

    Set<Long> toRemove = new HashSet<>(oldOptionIds);
    toRemove.removeAll(newOptionIds);

    //cần xóa option và giảm count
    for (Long optionId : toRemove) {
      voteRepo.deleteByPollIdAndUserIdAndOptionId(pollId, userId, optionId);
      optionRepo.addCount(optionId, -1);
    }

    //cần thêm option và tăng count
    for (Long optionId : toAdd) {
      voteRepo.save(LivePollVote.builder()
        .pollId(pollId)
        .userId(userId)
        .optionId(optionId)
        .createdAt(LocalDateTime.now())
        .build());

      optionRepo.addCount(optionId, 1);
    }

    PollResultResponse result = buildResult( pollId);
    livePollBroadcastService.broadcastPollResult(liveStreamId, result);
    return result;
  }

  @Transactional(readOnly = true)
  public PollResultResponse buildResult(Long pollId) {
    List<LivePollOption> options = optionRepo.findByPollIdOrderByIdAsc(pollId);
    long totalVotes = voteRepo.countByPollId(pollId);

    return PollResultResponse.builder()
      .pollId(pollId)
      .totalVotes(totalVotes)
      .options(pollMapper.toOptionResponses(options))
      .build();
  }

  @Transactional(readOnly = true)
  public PollOptionVotersResponse getOptionVoters(Long liveStreamId, Long pollId, Long optionId) {
    pollRepo.findByIdAndLiveStreamId(pollId, liveStreamId)
      .orElseThrow(() -> ExceptionUtil.notFound(ErrorMessage.POLL_NOT_FOUND));

    LivePollOption option = optionRepo.findByIdAndPollId(optionId, pollId)
      .orElseThrow(() -> ExceptionUtil.badRequest(ErrorMessage.POLL_OPTION_INVALID));

    List<LivePollVote> votes = voteRepo.findAllByPollIdAndOptionIdOrderByCreatedAtDesc(pollId, optionId);
    var userInfoMap = fetchUserInfoMap(votes);

    List<PollOptionVoterResponse> voters = votes.stream()
      .map(vote -> toPollOptionVoterResponse(vote, userInfoMap.get(vote.getUserId())))
      .toList();

    return PollOptionVotersResponse.builder()
      .optionId(option.getId())
      .optionText(option.getText())
      .voteCount(option.getCount())
      .voters(voters)
      .build();
  }

  private LivePollSetting settingDefault() {
    return LivePollSetting.builder()
      .allowChangeVote(false)
      .multipleChoice(false)
      .maxChoices(1)
      .resultVisibility(com.eefood.reactionservice.livestream.enums.PollResultVisibility.AFTER_VOTE)
      .voterVisibility(com.eefood.reactionservice.livestream.enums.PollVoterVisibility.ANONYMOUS)
      .optionAddMode(com.eefood.reactionservice.livestream.enums.PollOptionAddMode.HOST_ONLY)
      .build();
  }

  private void normalizeSetting(LivePollSetting s) {
    if (s.getMaxChoices() == null) s.setMaxChoices(1);
    if (Boolean.TRUE.equals(s.getMultipleChoice())) {
      if (s.getMaxChoices() < 2) s.setMaxChoices(2);
    } else {
      s.setMaxChoices(1);
    }
  }

  private java.util.Map<Long, UserInfo> fetchUserInfoMap(List<LivePollVote> votes) {
    List<Long> userIds = votes.stream()
      .map(LivePollVote::getUserId)
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

  private PollOptionVoterResponse toPollOptionVoterResponse(LivePollVote vote, UserInfo userInfo) {
    if (userInfo == null) {
      return PollOptionVoterResponse.builder()
        .userId(vote.getUserId())
        .username("Unknown")
        .avatarUrl(null)
        .votedAt(vote.getCreatedAt())
        .build();
    }

    return PollOptionVoterResponse.builder()
      .userId(userInfo.getId())
      .username(userInfo.getUsername())
      .avatarUrl(userInfo.getAvatarUrl())
      .votedAt(vote.getCreatedAt())
      .build();
  }
}
