package com.eefood.reactionservice.service;

import com.eefood.reactionservice.config.LiveKitConfig;
import com.eefood.reactionservice.dto.response.LiveStreamResponse;
import com.eefood.reactionservice.enums.LiveStreamStatus;
import com.eefood.reactionservice.mapper.LiveStreamMapper;
import com.eefood.reactionservice.model.livestream.*;
import com.eefood.reactionservice.repository.livestream.LiveStreamRepository;
import io.livekit.server.AccessToken;
import io.livekit.server.RoomServiceClient;
import io.livekit.server.RoomJoin;
import io.livekit.server.RoomName;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class LiveStreamService {
  private final LiveStreamRepository liveStreamRepository;
  private final LiveStreamMapper liveStreamMapper;
  private final RoomServiceClient roomServiceClient;
  private final LiveKitConfig liveKitConfig;

  @Transactional
  public LiveStreamResponse startLiveStream(Long userId, String requestTitle) {
    try {
      String roomName = "live_" + userId + "_" + System.currentTimeMillis();

      var roomCall = roomServiceClient.createRoom(roomName);
      var responseRoom = roomCall.execute();
      var room = responseRoom.body();

      if (room == null) {
        throw new RuntimeException("Failed to create LiveKit room");
      }

      // Tạo LiveStream
      LiveStream liveStream = new LiveStream();
      liveStream.setTitle(requestTitle);
      liveStream.setUserId(userId);
      liveStream.setRoomName(roomName);
      liveStream.setStatus(LiveStreamStatus.LIVE);
      liveStream.setStartedAt(LocalDateTime.now());
      liveStream.setLivekitRoomSid(room.getSid());

      liveStream = liveStreamRepository.save(liveStream);

      // Generate token cho streamer
      String token = generateToken(roomName, userId.toString(), true);

      LiveStreamResponse response = liveStreamMapper.toResponse(liveStream);
      response.setLivekitToken(token);

      log.info("Live stream started: {}", liveStream.getId());
      return response;

    } catch (Exception e) {
      log.error("Error starting live stream", e);
      throw new RuntimeException("Cannot start live stream: " + e.getMessage());
    }
  }

  @Transactional
  public void endLiveStream(Long liveStreamId, Long userId) {
    LiveStream liveStream = findLiveStreamById(liveStreamId);

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

    log.info("Live stream ended: {}", liveStreamId);
  }

  @Transactional(readOnly = true)
  public LiveStreamResponse getLiveStream(Long liveStreamId) {
    LiveStream liveStream = findLiveStreamById(liveStreamId);
    return liveStreamMapper.toResponse(liveStream);
  }

  public LiveStream findLiveStreamById(Long liveStreamId) {
    return liveStreamRepository.findById(liveStreamId)
      .orElseThrow(() -> new RuntimeException("Live stream not found"));
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