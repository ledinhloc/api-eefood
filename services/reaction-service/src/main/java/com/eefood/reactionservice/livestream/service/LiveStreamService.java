package com.eefood.reactionservice.livestream.service;

import com.eefood.reactionservice.config.LiveKitConfig;
import com.eefood.reactionservice.dto.response.UserInfo;
import com.eefood.reactionservice.livestream.dto.response.LiveStreamResponse;
import com.eefood.reactionservice.enums.ErrorMessage;
import com.eefood.reactionservice.enums.LiveStreamStatus;
import com.eefood.reactionservice.exception.ExceptionUtil;
import com.eefood.reactionservice.livestream.dto.ws.LiveStreamEndMessage;
import com.eefood.reactionservice.livestream.model.LiveStream;
import com.eefood.reactionservice.livestream.mapper.LiveStreamMapper;
import com.eefood.reactionservice.livestream.repository.LiveStreamBlockRepository;
import com.eefood.reactionservice.livestream.repository.LiveStreamRepository;
import com.eefood.reactionservice.repository.httpclient.IamClient;
import io.livekit.server.AccessToken;
import io.livekit.server.RoomServiceClient;
import io.livekit.server.RoomJoin;
import io.livekit.server.RoomName;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class LiveStreamService {
  private final LiveStreamRepository liveStreamRepository;
  private final LiveStreamMapper liveStreamMapper;
  private final RoomServiceClient roomServiceClient;
  private final LiveKitConfig liveKitConfig;
  private final SimpMessagingTemplate messagingTemplate;
  private final LiveStreamBlockRepository liveStreamBlockRepository;
  private final IamClient iamClient;

  @Transactional(readOnly = true)
  public LiveStreamResponse checkUserStream(Long currentUserId,Long userId) {
    boolean isBlocked = isUserBlockedByStreamer(userId, currentUserId);

    if(isBlocked) {
      log.info(
        "User {} is blocked by streamer {}. Hide livestream",
        currentUserId, userId
      );

      return null;
    }

    LiveStream live = liveStreamRepository
      .findTopByUserIdAndStatusInOrderByIdDesc(
        userId,
        List.of(LiveStreamStatus.SCHEDULED, LiveStreamStatus.LIVE)
      );

    if (live == null) {
      return null;
    }

    LiveStreamResponse res = liveStreamMapper.toResponse(live);
    return res;
  }

  public LiveStreamResponse scheduleLive(Long userId, String description, LocalDateTime time) {
    LiveStream liveStream = new LiveStream();
    liveStream.setUserId(userId);
    liveStream.setTitle(description);
    liveStream.setScheduledAt(time);
    liveStream.setStatus(LiveStreamStatus.SCHEDULED);
    liveStreamRepository.save(liveStream);
    return liveStreamMapper.toResponse(liveStream);
  }

  @Transactional
  public LiveStreamResponse startLiveStream(Long userId, Long liveStreamId, String requestTitle) {
    try {
      LiveStream live;
      //Nếu có id lịch livestream
      if(liveStreamId != null) {
        live = liveStreamRepository.findById(liveStreamId).orElseThrow(() -> new RuntimeException("Live Stream Not Found"));
        if(!live.getUserId().equals(userId)) {
          throw ExceptionUtil.forbidden(ErrorMessage.ACCESS_DENIED);
        }

        // Nếu đang LIVE → trả về luôn
        if (live.getStatus() == LiveStreamStatus.LIVE) {
          return buildLiveResponse(live, userId);
        }
        // Update lịch thành LIVE
        live.setStatus(LiveStreamStatus.LIVE);
        live.setStartedAt(LocalDateTime.now());
        if (requestTitle != null) live.setTitle(requestTitle);
      }
      else {
        //Không có id → check user đang live chưa
        live = liveStreamRepository.findTopByUserIdAndStatusInOrderByIdDesc(
          userId,
          List.of(LiveStreamStatus.LIVE)
        );

        if (live != null) {
          return buildLiveResponse(live, userId);
        }

        // Tạo live mới
        live = new LiveStream();
        live.setUserId(userId);
        live.setTitle(requestTitle);
        live.setStatus(LiveStreamStatus.LIVE);
        live.setStartedAt(LocalDateTime.now());
      }
      String roomName = "live_" + userId + "_" + System.currentTimeMillis();

      var roomCall = roomServiceClient.createRoom(roomName);
      var responseRoom = roomCall.execute();
      if (!responseRoom.isSuccessful()) {
        throw new RuntimeException("Failed to create LiveKit room: HTTP " + responseRoom.code());
      }
      var room = responseRoom.body();

      if (room == null) {
        throw new RuntimeException("Failed to create LiveKit room");
      }

      live.setRoomName(roomName);
      live.setLivekitRoomSid(room.getSid());

      liveStreamRepository.save(live);
      log.info("Live stream started: {}", live.getId());

      return buildLiveResponse(live, userId);
    } catch (Exception e) {
      log.error("Error starting live stream", e);
      throw new RuntimeException("Cannot start live stream: " + e.getMessage());
    }
  }

  @Transactional
  public LiveStreamResponse endLiveStream(Long liveStreamId, Long userId) {
    LiveStream liveStream = liveStreamRepository.findById(liveStreamId)
      .orElseThrow(() -> new RuntimeException("Live stream not found"));

//    System.out.printf("Live stream ended: %s %s\n", liveStream.getUserId(), userId);
    if (!liveStream.getUserId().equals(userId)) {
      throw new RuntimeException("Only stream owner can end the stream");
    }

    liveStream.setStatus(LiveStreamStatus.ENDED);
    liveStream.setEndedAt(LocalDateTime.now());
    liveStream.setViewerCount(0);
    liveStreamRepository.save(liveStream);

    // Delete LiveKit room
    try {
      roomServiceClient.deleteRoom(liveStream.getRoomName());
    } catch (Exception e) {
      log.error("Error deleting LiveKit room", e);
    }

    broadcastStreamEnded(liveStreamId, liveStream.getEndedAt());

    log.info("Live stream ended: {}", liveStreamId);
    return liveStreamMapper.toResponse(liveStream);
  }

  private void broadcastStreamEnded(Long liveStreamId, LocalDateTime endedAt) {
    try {
      LiveStreamEndMessage message = LiveStreamEndMessage.builder()
        .type("STREAM_ENDED")
        .liveStreamId(liveStreamId)
        .message("Phiên phát trực tiếp đã kết thúc")
        .endedAt(endedAt)
        .build();

      // Gửi đến topic chung của livestream
      messagingTemplate.convertAndSend(
        "/topic/livestream/" + liveStreamId,
        message
      );

      log.info("Broadcasted STREAM_ENDED for livestream {}", liveStreamId);

    } catch (Exception e) {
      log.error("Error broadcasting stream ended", e);
    }
  }

  @Transactional(readOnly = true)
  public LiveStreamResponse getLiveStream(Long liveStreamId, Long userId) {
    LiveStream liveStream = getLiveStreamEntity(liveStreamId);

    boolean isBlocked = isUserBlockedByStreamer(liveStream.getUserId(), userId);
    if(isBlocked) {
      log.warn(
        "Blocked user {} tried to access livestream {} of streamer {}",
        userId,
        liveStreamId,
        liveStream.getUserId()
      );
      return null;
    }

    String viewerToken = generateViewerToken(liveStream.getRoomName(),userId);
    LiveStreamResponse res = liveStreamMapper.toResponse(liveStream);
    res.setLivekitToken(viewerToken);
    UserInfo user = iamClient.getUserInfo(res.getUserId()).getData();
    if(user != null) {
      res.setUsername(user.getUsername());
      res.setEmail(user.getEmail());
      res.setAvatarUrl(user.getAvatarUrl());
    }
    return res;
  }

  @Transactional(readOnly = true)
  public LiveStream getLiveStreamEntity(Long liveStreamId) {
    return liveStreamRepository.findById(liveStreamId)
      .orElseThrow(() -> new RuntimeException("Live stream not found"));
  }

  @Transactional(readOnly = true)
  public boolean isLiveStreamOwnedByStreamer(Long liveStreamId, Long streamerId) {
    LiveStream liveStream = getLiveStreamEntity(liveStreamId);
    return liveStream.getUserId().equals(streamerId);
  }

  @Transactional(readOnly = true)
  public boolean isUserBlockedByStreamer(Long streamerId, Long userId) {
    return liveStreamBlockRepository.existsByStreamerIdAndBlockedUserId(streamerId, userId);
  }

  private LiveStreamResponse buildLiveResponse(LiveStream live, Long userId) {
    String token = generateStreamerToken(live.getRoomName(), userId);
    LiveStreamResponse res = liveStreamMapper.toResponse(live);
    res.setLivekitToken(token);
    return res;
  }

  /**
   * Generate JWT token cho LiveKit
   * @param roomName Tên phòng
   * @param identity Identity của user (userId)
   * @param canPublish Có quyền publish stream không (true cho streamer, false cho viewer)
   * @return JWT token
   */
  public String generateToken(String roomName, String identity, boolean canPublish) {
    AccessToken token = new AccessToken(liveKitConfig.getApiKey(), liveKitConfig.getApiSecret());
    token.setName(identity);
    token.setIdentity(identity);

    // Thêm grants: RoomJoin để join phòng và RoomName để chỉ định phòng cụ thể
    token.addGrants(new RoomJoin(true), new RoomName(roomName));

    // Set metadata để lưu thông tin canPublish
    token.setMetadata("{\"canPublish\":" + canPublish + "}");

    return token.toJwt();
  }

  /**
   * Generate token cho viewer (không có quyền publish)
   */
  public String generateViewerToken(String roomName, Long userId) {
    return generateToken(roomName, "viewer_" + userId, false);
  }

  /**
   * Generate token cho streamer (có quyền publish)
   */
  public String generateStreamerToken(String roomName, Long userId) {
    return generateToken(roomName, "streamer_" + userId, true);
  }
}
