package com.eefood.reactionservice.livestream.service;


import com.eefood.reactionservice.enums.ErrorMessage;
import com.eefood.reactionservice.exception.ExceptionUtil;
import com.eefood.reactionservice.livestream.dto.request.CreateLivePollRequest;
import com.eefood.reactionservice.livestream.dto.response.LivePollResponse;
import com.eefood.reactionservice.livestream.dto.response.PollResultResponse;
import com.eefood.reactionservice.livestream.enums.PollStatus;
import com.eefood.reactionservice.livestream.mapper.LivePollMapper;
import com.eefood.reactionservice.livestream.model.*;
import com.eefood.reactionservice.livestream.repository.LivePollOptionRepository;
import com.eefood.reactionservice.livestream.repository.LivePollRepository;
import com.eefood.reactionservice.livestream.repository.LivePollSettingRepository;
import com.eefood.reactionservice.livestream.repository.LivePollVoteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LivePollService {

  private final LivePollRepository pollRepo;
  private final LivePollOptionRepository optionRepo;
  private final LivePollSettingRepository settingRepo;
  private final LivePollVoteRepository voteRepo;

  private final LivePollMapper pollMapper;

  @Transactional
  public LivePollResponse create(CreateLivePollRequest req) {
    if (req == null) throw ExceptionUtil.badRequest(ErrorMessage.INVALID_REQUEST);
    if (req.getLiveStreamId() == null) throw ExceptionUtil.badRequest(ErrorMessage.INVALID_LIVESTREAM_ID);
    if (req.getQuestion() == null || req.getQuestion().trim().isBlank()) {
      throw ExceptionUtil.badRequest(ErrorMessage.INVALID_POLL_QUESTION);
    }
    if (req.getOptions() == null || req.getOptions().size() < 2) {
      throw ExceptionUtil.badRequest(ErrorMessage.INVALID_POLL_OPTIONS);
    }

    LivePoll poll = pollRepo.save(LivePoll.builder()
      .liveStreamId(req.getLiveStreamId())
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

    // setting default (v1)
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
  public PollResultResponse vote(Long liveStreamId, Long pollId, Long userId, Long optionId) {
    if (optionId == null) {
      throw ExceptionUtil.badRequest(ErrorMessage.INVALID_REQUEST);
    }

    LivePoll poll = pollRepo.findByIdAndLiveStreamId(pollId, liveStreamId)
      .orElseThrow(() -> ExceptionUtil.notFound(ErrorMessage.POLL_NOT_FOUND));

    if (poll.getStatus() != PollStatus.OPEN) {
      throw ExceptionUtil.badRequest(ErrorMessage.POLL_NOT_OPEN);
    }

    LivePollSetting setting = settingRepo.findByPollId(pollId)
      .orElseThrow(() -> ExceptionUtil.notFound(ErrorMessage.POLL_SETTING_NOT_FOUND));

    LivePollOption selected = optionRepo.findByIdAndPollId(optionId, pollId)
      .orElseThrow(() -> ExceptionUtil.badRequest(ErrorMessage.POLL_OPTION_INVALID));

    // schema vote đang là 1 user 1 option, không support multipleChoice thật
    if (Boolean.TRUE.equals(setting.getMultipleChoice())) {
      throw ExceptionUtil.badRequest(ErrorMessage.POLL_MULTIPLE_CHOICE_NOT_SUPPORTED);
    }

    var existing = voteRepo.findByPollIdAndUserId(pollId, userId);

    if (existing.isEmpty()) {
      try {
        voteRepo.save(LivePollVote.builder()
          .pollId(pollId)
          .userId(userId)
          .optionId(selected.getId())
          .createdAt(LocalDateTime.now())
          .build());
      } catch (DataIntegrityViolationException e) {
        throw ExceptionUtil.conflict(ErrorMessage.POLL_ALREADY_VOTED);
      }
      optionRepo.addCount(selected.getId(), 1);
      return buildResult(pollId);
    }

    if (!Boolean.TRUE.equals(setting.getAllowChangeVote())) {
      throw ExceptionUtil.conflict(ErrorMessage.POLL_ALREADY_VOTED);
    }

    //neu trung lua chon cu
    LivePollVote v = existing.get();
    if (v.getOptionId().equals(selected.getId())) {
      return buildResult(pollId);
    }

    optionRepo.addCount(v.getOptionId(), -1);
    optionRepo.addCount(selected.getId(), 1);
    v.setOptionId(selected.getId());
    voteRepo.save(v);

    return buildResult(pollId);
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
}