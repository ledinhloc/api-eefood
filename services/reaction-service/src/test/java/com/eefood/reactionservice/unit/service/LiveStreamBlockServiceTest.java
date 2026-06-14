package com.eefood.reactionservice.unit.service;

import com.eefood.reactionservice.dto.response.ResponseData;
import com.eefood.reactionservice.dto.response.UserInfo;
import com.eefood.reactionservice.livestream.dto.response.BlockUserResponse;
import com.eefood.reactionservice.livestream.dto.ws.LiveStreamEndMessage;
import com.eefood.reactionservice.livestream.mapper.LiveStreamBlockMapper;
import com.eefood.reactionservice.livestream.model.LiveStreamBlock;
import com.eefood.reactionservice.livestream.model.LiveView;
import com.eefood.reactionservice.livestream.repository.LiveStreamBlockRepository;
import com.eefood.reactionservice.livestream.service.LiveStreamBlockService;
import com.eefood.reactionservice.livestream.service.LiveStreamService;
import com.eefood.reactionservice.livestream.service.LiveViewerService;
import com.eefood.reactionservice.repository.httpclient.IamClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doThrow;

@ExtendWith(MockitoExtension.class)
class LiveStreamBlockServiceTest {

  @Mock
  private LiveStreamBlockRepository repo;

  @Mock
  private IamClient iamClient;

  @Mock
  private LiveStreamBlockMapper mapper;

  @Mock
  private SimpMessagingTemplate messagingTemplate;

  @Mock
  private LiveViewerService liveViewerService;

  @Mock
  private LiveStreamService liveStreamService;

  @InjectMocks
  private LiveStreamBlockService service;

  @Test
  void blockUser_userAlreadyBlocked_throwConflict() {
    Long streamerId = 1L;
    Long blockedUserId = 2L;
    when(repo.existsByStreamerIdAndBlockedUserId(streamerId, blockedUserId))
      .thenReturn(true);

    ResponseStatusException exception = assertThrows(
      ResponseStatusException.class,
      () -> service.blockUser(streamerId, blockedUserId)
    );

    assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
    assertEquals("This user is already blocked by you", exception.getReason());
    verify(repo).existsByStreamerIdAndBlockedUserId(streamerId, blockedUserId);
    verify(repo, never()).save(any(LiveStreamBlock.class));
    verifyNoInteractions(
      iamClient,
      mapper,
      messagingTemplate,
      liveViewerService,
      liveStreamService
    );
  }

  @Test
  void blockUser_userInfoNotFound_throwNotFound() {
    Long streamerId = 1L;
    Long blockedUserId = 2L;
    when(repo.existsByStreamerIdAndBlockedUserId(streamerId, blockedUserId))
      .thenReturn(false);
    when(iamClient.getUserInfo(blockedUserId))
      .thenReturn(new ResponseData<>(200, "Success", null));

    ResponseStatusException exception = assertThrows(
      ResponseStatusException.class,
      () -> service.blockUser(streamerId, blockedUserId)
    );

    assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    verify(repo).save(any(LiveStreamBlock.class));
    verify(iamClient).getUserInfo(blockedUserId);
    verifyNoInteractions(
      mapper,
      messagingTemplate,
      liveViewerService,
      liveStreamService
    );
  }

  @Test
  void blockUser_activeViewerOfStreamer_blockAndRemoveViewerSuccessfully() {
    Long streamerId = 1L;
    Long blockedUserId = 2L;
    Long liveStreamId = 10L;

    UserInfo userInfo = UserInfo.builder()
      .id(blockedUserId)
      .username("viewer")
      .email("viewer@example.com")
      .avatarUrl("avatar.png")
      .build();
    ResponseData<UserInfo> userResponse = new ResponseData<>(200, "Success", userInfo);
    LiveView activeView = LiveView.builder()
      .userId(blockedUserId)
      .liveStreamId(liveStreamId)
      .build();
    BlockUserResponse expectedResponse = BlockUserResponse.builder()
      .blockedUserId(blockedUserId)
      .username("viewer")
      .email("viewer@example.com")
      .avatarUrl("avatar.png")
      .build();

    when(repo.existsByStreamerIdAndBlockedUserId(streamerId, blockedUserId))
      .thenReturn(false);
    when(iamClient.getUserInfo(blockedUserId)).thenReturn(userResponse);
    when(liveViewerService.findActiveViewByUserId(blockedUserId))
      .thenReturn(Optional.of(activeView));
    when(liveStreamService.isLiveStreamOwnedByStreamer(liveStreamId, streamerId))
      .thenReturn(true);
    when(mapper.toResponse(any(LiveStreamBlock.class), any(UserInfo.class)))
      .thenReturn(expectedResponse);

    BlockUserResponse actualResponse = service.blockUser(streamerId, blockedUserId);

    ArgumentCaptor<LiveStreamBlock> blockCaptor =
      ArgumentCaptor.forClass(LiveStreamBlock.class);
    verify(repo).save(blockCaptor.capture());
    LiveStreamBlock savedBlock = blockCaptor.getValue();
    assertEquals(streamerId, savedBlock.getStreamerId());
    assertEquals(blockedUserId, savedBlock.getBlockedUserId());
    assertNotNull(savedBlock.getCreatedAt());

    verify(liveViewerService).leaveLive(liveStreamId, blockedUserId);

    ArgumentCaptor<LiveStreamEndMessage> messageCaptor =
      ArgumentCaptor.forClass(LiveStreamEndMessage.class);
    verify(messagingTemplate).convertAndSendToUser(
      org.mockito.ArgumentMatchers.eq(blockedUserId.toString()),
      org.mockito.ArgumentMatchers.eq("/queue/livestream"),
      messageCaptor.capture()
    );
    LiveStreamEndMessage message = messageCaptor.getValue();
    assertEquals("STREAM_ENDED", message.getType());
    assertEquals(liveStreamId, message.getLiveStreamId());
    assertNotNull(message.getEndedAt());

    verify(mapper).toResponse(savedBlock, userInfo);
    assertEquals(expectedResponse, actualResponse);
    assertTrue(actualResponse.getBlockedUserId().equals(blockedUserId));
  }

  @Test
  void unblockUser_userNotBlocked_throwNotFound() {
    Long streamerId = 1L;
    Long blockedUserId = 2L;
    when(repo.existsByStreamerIdAndBlockedUserId(streamerId, blockedUserId))
      .thenReturn(false);

    ResponseStatusException exception = assertThrows(
      ResponseStatusException.class,
      () -> service.unblockUser(streamerId, blockedUserId)
    );

    assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    assertEquals("This user is not blocked", exception.getReason());
    verify(repo).existsByStreamerIdAndBlockedUserId(streamerId, blockedUserId);
    verify(repo, never()).deleteByStreamerIdAndBlockedUserId(
      streamerId,
      blockedUserId
    );
    verifyNoInteractions(
      iamClient,
      mapper,
      messagingTemplate,
      liveViewerService,
      liveStreamService
    );
  }

  @Test
  void unblockUser_userIsBlocked_deleteSuccessfully() {
    Long streamerId = 1L;
    Long blockedUserId = 2L;
    when(repo.existsByStreamerIdAndBlockedUserId(streamerId, blockedUserId))
      .thenReturn(true);

    service.unblockUser(streamerId, blockedUserId);

    verify(repo).existsByStreamerIdAndBlockedUserId(streamerId, blockedUserId);
    verify(repo).deleteByStreamerIdAndBlockedUserId(streamerId, blockedUserId);
    verifyNoInteractions(
      iamClient,
      mapper,
      messagingTemplate,
      liveViewerService,
      liveStreamService
    );
  }
}
