package com.eefood.reactionservice.livestream.service;

import com.eefood.reactionservice.enums.ErrorMessage;
import com.eefood.reactionservice.exception.ExceptionUtil;
import com.eefood.reactionservice.livestream.dto.cache.PollVoteMetadata;
import com.eefood.reactionservice.livestream.model.LivePoll;
import com.eefood.reactionservice.livestream.model.LivePollOption;
import com.eefood.reactionservice.livestream.model.LivePollSetting;
import com.eefood.reactionservice.livestream.repository.LivePollOptionRepository;
import com.eefood.reactionservice.livestream.repository.LivePollRepository;
import com.eefood.reactionservice.livestream.repository.LivePollSettingRepository;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LivePollMetadataCacheService {
  public static final String POLL_VOTE_METADATA_CACHE = "poll-vote-metadata";

  private final LivePollRepository pollRepo;
  private final LivePollSettingRepository settingRepo;
  private final LivePollOptionRepository optionRepo;

  @Cacheable(cacheNames = POLL_VOTE_METADATA_CACHE, key = "#pollId")
  public PollVoteMetadata getPollVoteMetadata(Long pollId) {
    LivePoll poll = pollRepo.findById(pollId)
      .orElseThrow(() -> ExceptionUtil.notFound(ErrorMessage.POLL_NOT_FOUND));

    LivePollSetting setting = settingRepo.findByPollId(pollId)
      .orElseThrow(() -> ExceptionUtil.notFound(ErrorMessage.POLL_SETTING_NOT_FOUND));

    Set<Long> optionIds = optionRepo.findByPollIdOrderByIdAsc(pollId).stream()
      .map(LivePollOption::getId)
      .collect(Collectors.toSet());

    return PollVoteMetadata.builder()
      .pollId(poll.getId())
      .liveStreamId(poll.getLiveStreamId())
      .status(poll.getStatus())
      .multipleChoice(setting.getMultipleChoice())
      .allowChangeVote(setting.getAllowChangeVote())
      .maxChoices(setting.getMaxChoices())
      .optionIds(optionIds)
      .build();
  }

  @CacheEvict(cacheNames = POLL_VOTE_METADATA_CACHE, key = "#pollId")
  public void evictPollVoteMetadata(Long pollId) {
    // Spring cache eviction hook
  }
}
