package com.eefood.reactionservice.unit.service;

import com.eefood.reactionservice.livestream.dto.cache.PollVoteMetadata;
import com.eefood.common.avro.LivePollVoteEvent;
import com.eefood.reactionservice.livestream.dto.request.CreateLivePollRequest;
import com.eefood.reactionservice.livestream.dto.response.LivePollResponse;
import com.eefood.reactionservice.livestream.dto.response.PollResultResponse;
import com.eefood.reactionservice.livestream.enums.PollOptionAddMode;
import com.eefood.reactionservice.livestream.enums.PollResultVisibility;
import com.eefood.reactionservice.livestream.enums.PollStatus;
import com.eefood.reactionservice.livestream.enums.PollVoterVisibility;
import com.eefood.reactionservice.livestream.mapper.LivePollMapper;
import com.eefood.reactionservice.livestream.model.LivePoll;
import com.eefood.reactionservice.livestream.model.LivePollOption;
import com.eefood.reactionservice.livestream.model.LivePollSetting;
import com.eefood.reactionservice.livestream.repository.LivePollOptionRepository;
import com.eefood.reactionservice.livestream.repository.LivePollRepository;
import com.eefood.reactionservice.livestream.repository.LivePollSettingRepository;
import com.eefood.reactionservice.livestream.repository.LivePollVoteRepository;
import com.eefood.reactionservice.livestream.service.LivePollBroadcastService;
import com.eefood.reactionservice.livestream.service.LivePollMetadataCacheService;
import com.eefood.reactionservice.livestream.service.LivePollResultCacheService;
import com.eefood.reactionservice.livestream.service.LivePollService;
import com.eefood.reactionservice.livestream.service.LivePollVoteStateCacheService;
import com.eefood.reactionservice.livestream.service.LivePollVoteKafkaProducer;
import com.eefood.reactionservice.repository.httpclient.IamClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class LivePollServiceTest {

  @Mock
  private LivePollRepository pollRepo;

  @Mock
  private LivePollOptionRepository optionRepo;

  @Mock
  private LivePollSettingRepository settingRepo;

  @Mock
  private LivePollVoteRepository voteRepo;

  @Mock
  private LivePollMetadataCacheService livePollMetadataCacheService;

  @Mock
  private LivePollResultCacheService livePollResultCacheService;

  @Mock
  private LivePollVoteStateCacheService livePollVoteStateCacheService;

  @Mock
  private LivePollVoteKafkaProducer livePollVoteKafkaProducer;

  @Mock
  private LivePollMapper pollMapper;

  @Mock
  private LivePollBroadcastService livePollBroadcastService;

  @Mock
  private IamClient iamClient;

  @InjectMocks
  private LivePollService service;

  @Test
  void create_containsBlankOption_throwBadRequest() {
    Long liveStreamId = 10L;
    Long pollId = 20L;
    CreateLivePollRequest request = CreateLivePollRequest.builder()
      .question("Món ăn yêu thích?")
      .options(List.of("Phở", "   "))
      .build();
    when(pollRepo.save(any(LivePoll.class)))
      .thenAnswer(invocation -> {
        LivePoll poll = invocation.getArgument(0);
        poll.setId(pollId);
        return poll;
      });

    ResponseStatusException exception = assertThrows(
      ResponseStatusException.class,
      () -> service.create(request, liveStreamId)
    );

    assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    assertEquals("Invalid poll options", exception.getReason());
    verify(pollRepo).save(any(LivePoll.class));

    ArgumentCaptor<LivePollOption> optionCaptor =
      ArgumentCaptor.forClass(LivePollOption.class);
    verify(optionRepo).save(optionCaptor.capture());
    assertEquals(pollId, optionCaptor.getValue().getPollId());
    assertEquals("Phở", optionCaptor.getValue().getText());
    assertEquals(0L, optionCaptor.getValue().getCount());

    verify(settingRepo, never()).save(any(LivePollSetting.class));
    verifyNoInteractions(
      voteRepo,
      livePollMetadataCacheService,
      livePollResultCacheService,
      livePollVoteStateCacheService,
      livePollVoteKafkaProducer,
      pollMapper,
      livePollBroadcastService,
      iamClient
    );
  }

  @Test
  void create_multipleChoiceWithInvalidMaxChoices_normalizeAndCreateSuccessfully() {
    Long liveStreamId = 10L;
    Long pollId = 20L;
    CreateLivePollRequest request = CreateLivePollRequest.builder()
      .question("  Chọn các món ăn yêu thích  ")
      .options(List.of("  Phở  ", "Bún bò", "Cơm tấm"))
      .allowChangeVote(true)
      .multipleChoice(true)
      .maxChoices(1)
      .resultVisibility(PollResultVisibility.AFTER_CLOSE)
      .voterVisibility(PollVoterVisibility.PUBLIC)
      .optionAddMode(PollOptionAddMode.VIEWER_WITH_APPROVAL)
      .build();
    LivePollResponse expectedResponse = LivePollResponse.builder()
      .id(pollId)
      .liveStreamId(liveStreamId)
      .question("Chọn các món ăn yêu thích")
      .status(PollStatus.DRAFT)
      .build();

    when(pollRepo.save(any(LivePoll.class)))
      .thenAnswer(invocation -> {
        LivePoll poll = invocation.getArgument(0);
        poll.setId(pollId);
        return poll;
      });
    when(optionRepo.findByPollIdOrderByIdAsc(pollId))
      .thenReturn(List.of());
    when(pollMapper.toFullResponse(
      any(LivePoll.class),
      any(LivePollSetting.class),
      any()
    )).thenReturn(expectedResponse);

    LivePollResponse actualResponse = service.create(request, liveStreamId);

    ArgumentCaptor<LivePoll> pollCaptor = ArgumentCaptor.forClass(LivePoll.class);
    verify(pollRepo).save(pollCaptor.capture());
    LivePoll savedPoll = pollCaptor.getValue();
    assertEquals(liveStreamId, savedPoll.getLiveStreamId());
    assertEquals("Chọn các món ăn yêu thích", savedPoll.getQuestion());
    assertEquals(PollStatus.DRAFT, savedPoll.getStatus());

    ArgumentCaptor<LivePollOption> optionCaptor =
      ArgumentCaptor.forClass(LivePollOption.class);
    verify(optionRepo, org.mockito.Mockito.times(3)).save(optionCaptor.capture());
    List<LivePollOption> savedOptions = optionCaptor.getAllValues();
    assertEquals(List.of("Phở", "Bún bò", "Cơm tấm"), savedOptions.stream()
      .map(LivePollOption::getText)
      .toList());
    assertEquals(List.of(0L, 0L, 0L), savedOptions.stream()
      .map(LivePollOption::getCount)
      .toList());
    assertEquals(List.of(pollId, pollId, pollId), savedOptions.stream()
      .map(LivePollOption::getPollId)
      .toList());

    ArgumentCaptor<LivePollSetting> settingCaptor =
      ArgumentCaptor.forClass(LivePollSetting.class);
    verify(settingRepo).save(settingCaptor.capture());
    LivePollSetting savedSetting = settingCaptor.getValue();
    assertEquals(pollId, savedSetting.getPollId());
    assertEquals(true, savedSetting.getAllowChangeVote());
    assertEquals(true, savedSetting.getMultipleChoice());
    assertEquals(2, savedSetting.getMaxChoices());
    assertEquals(PollResultVisibility.AFTER_CLOSE, savedSetting.getResultVisibility());
    assertEquals(PollVoterVisibility.PUBLIC, savedSetting.getVoterVisibility());
    assertEquals(PollOptionAddMode.VIEWER_WITH_APPROVAL, savedSetting.getOptionAddMode());

    verify(optionRepo).findByPollIdOrderByIdAsc(pollId);
    verify(pollMapper).toFullResponse(savedPoll, savedSetting, List.of());
    assertEquals(expectedResponse, actualResponse);
    verifyNoInteractions(
      voteRepo,
      livePollMetadataCacheService,
      livePollResultCacheService,
      livePollVoteStateCacheService,
      livePollVoteKafkaProducer,
      livePollBroadcastService,
      iamClient
    );
  }

  @Test
  void vote_emptyOptionIds_throwBadRequest() {
    Long liveStreamId = 10L;
    Long pollId = 20L;
    Long userId = 30L;

    ResponseStatusException exception = assertThrows(
      ResponseStatusException.class,
      () -> service.vote(liveStreamId, pollId, userId, Collections.emptyList())
    );

    assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    assertEquals("Invalid request data", exception.getReason());
    verifyNoInteractions(
      pollRepo,
      optionRepo,
      settingRepo,
      voteRepo,
      livePollMetadataCacheService,
      livePollResultCacheService,
      livePollVoteStateCacheService,
      livePollVoteKafkaProducer,
      pollMapper,
      livePollBroadcastService,
      iamClient
    );
  }

  @Test
  void vote_invalidOptionId_throwBadRequest() {
    Long liveStreamId = 10L;
    Long pollId = 20L;
    Long userId = 30L;
    Long invalidOptionId = 999L;
    PollVoteMetadata metadata = singleChoiceMetadata(
      pollId,
      liveStreamId,
      Set.of(101L, 102L)
    );
    when(livePollMetadataCacheService.getPollVoteMetadata(pollId))
      .thenReturn(metadata);

    ResponseStatusException exception = assertThrows(
      ResponseStatusException.class,
      () -> service.vote(
        liveStreamId,
        pollId,
        userId,
        List.of(invalidOptionId)
      )
    );

    assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    assertEquals("Invalid poll option", exception.getReason());
    verify(livePollMetadataCacheService).getPollVoteMetadata(pollId);
    verifyNoInteractions(
      livePollResultCacheService,
      livePollVoteStateCacheService,
      livePollVoteKafkaProducer,
      livePollBroadcastService
    );
  }

  @Test
  void vote_singleChoiceFirstVote_createVoteSuccessfully() {
    Long liveStreamId = 10L;
    Long pollId = 20L;
    Long userId = 30L;
    Long optionId = 101L;
    PollVoteMetadata metadata = singleChoiceMetadata(
      pollId,
      liveStreamId,
      Set.of(optionId, 102L)
    );
    PollResultResponse expectedResult = PollResultResponse.builder()
      .pollId(pollId)
      .options(List.of())
      .build();

    when(livePollMetadataCacheService.getPollVoteMetadata(pollId))
      .thenReturn(metadata);
    when(livePollVoteStateCacheService.getOptionIds(pollId, userId))
      .thenReturn(Collections.emptySet());
    when(livePollResultCacheService.applyVoteDelta(
      eq(pollId),
      any()
    )).thenReturn(expectedResult);

    PollResultResponse actualResult = service.vote(
      liveStreamId,
      pollId,
      userId,
      List.of(optionId)
    );

    verify(livePollVoteStateCacheService)
      .saveOptionIds(pollId, userId, Set.of(optionId));

    @SuppressWarnings("unchecked")
    ArgumentCaptor<Map<Long, Long>> deltaCaptor =
      ArgumentCaptor.forClass(Map.class);
    verify(livePollResultCacheService)
      .applyVoteDelta(eq(pollId), deltaCaptor.capture());
    assertEquals(Map.of(optionId, 1L), deltaCaptor.getValue());

    ArgumentCaptor<LivePollVoteEvent> eventCaptor =
      ArgumentCaptor.forClass(LivePollVoteEvent.class);
    verify(livePollVoteKafkaProducer).publishVoteEvent(eventCaptor.capture());
    LivePollVoteEvent event = eventCaptor.getValue();
    assertNotNull(event.getEventId());
    assertEquals(liveStreamId, event.getLiveStreamId());
    assertEquals(pollId, event.getPollId());
    assertEquals(userId, event.getUserId());
    assertEquals(List.of(optionId), event.getToAdd());
    assertEquals(List.of(), event.getToRemove());
    assertEquals(true, event.getOccurredAt() > 0);

    verify(livePollBroadcastService)
      .broadcastPollResult(liveStreamId, expectedResult);
    assertEquals(expectedResult, actualResult);
    verifyNoInteractions(pollRepo, optionRepo, settingRepo, voteRepo, pollMapper, iamClient);
  }

  private PollVoteMetadata singleChoiceMetadata(
    Long pollId,
    Long liveStreamId,
    Set<Long> optionIds
  ) {
    return PollVoteMetadata.builder()
      .pollId(pollId)
      .liveStreamId(liveStreamId)
      .status(PollStatus.OPEN)
      .multipleChoice(false)
      .allowChangeVote(false)
      .maxChoices(1)
      .optionIds(optionIds)
      .build();
  }
}
