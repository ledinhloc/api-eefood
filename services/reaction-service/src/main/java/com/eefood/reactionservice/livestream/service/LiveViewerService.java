package com.eefood.reactionservice.livestream.service;

import com.eefood.reactionservice.dto.response.UserInfo;
import com.eefood.reactionservice.livestream.dto.response.ViewerResponse;
import com.eefood.reactionservice.livestream.dto.ws.ViewerUpdateMessage;
import com.eefood.reactionservice.livestream.model.LiveStream;
import com.eefood.reactionservice.livestream.model.LiveView;
import com.eefood.reactionservice.livestream.repository.LiveStreamBlockRepository;
import com.eefood.reactionservice.livestream.repository.LiveStreamRepository;
import com.eefood.reactionservice.livestream.repository.LiveViewRepository;
import com.eefood.reactionservice.repository.httpclient.IamClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class LiveViewerService {
  private final LiveViewRepository liveViewRepository;
  private final IamClient iamClient;
  private final SimpMessagingTemplate messagingTemplate;
  private final LiveStreamRepository liveStreamRepository;
  private final LiveStreamBlockRepository liveStreamBlockRepository;

  @Transactional
  public void joinLive(Long liveStreamId, Long userId) {
    // 1. Lấy livestream
    LiveStream liveStream = liveStreamRepository
      .findById(liveStreamId)
      .orElseThrow(() -> new RuntimeException("Live stream not found"));

    Long streamerId = liveStream.getUserId();
    boolean isBlocked = liveStreamBlockRepository
      .existsByStreamerIdAndBlockedUserId(streamerId, userId);

    if (isBlocked) {
      log.warn(
        "Blocked user {} tried to join livestream {} of streamer {}",
        userId,
        liveStreamId,
        streamerId
      );
      return;

    }
    // Kiểm tra user đã join chưa
    boolean alreadyJoined = liveViewRepository
      .existsByLiveStreamIdAndUserIdAndLeftAtIsNull(liveStreamId, userId);

    if (alreadyJoined) {
      log.info("User {} already in livestream {}", userId, liveStreamId);
      return;
    }

    // Insert vào DB
    LiveView liveView = LiveView.builder()
      .userId(userId)
      .liveStreamId(liveStreamId)
      .joinedAt(LocalDateTime.now())
      .build();

    liveViewRepository.save(liveView);

    log.info("User {} joined livestream {}", userId, liveStreamId);

    // Broadcast WebSocket (chỉ gửi viewer mới join)
    broadcastViewerJoin(liveStreamId, userId, liveView.getJoinedAt());
  }

  @Transactional
  public void leaveLive(Long liveStreamId, Long userId) {
    var liveViewOpt = liveViewRepository
      .findByLiveStreamIdAndUserIdAndLeftAtIsNull(liveStreamId, userId);

    if (liveViewOpt.isEmpty()) {
      log.warn("User {} not in livestream {}", userId, liveStreamId);
      return;
    }

    LiveView liveView = liveViewOpt.get();
    liveView.setLeftAt(LocalDateTime.now());
    liveViewRepository.save(liveView);

    log.info("User {} left livestream {}", userId, liveStreamId);

    // Broadcast WebSocket (chỉ gửi userId rời đi)
    broadcastViewerLeave(liveStreamId, userId);
  }

  @Transactional(readOnly = true)
  public List<ViewerResponse> getCurrentViewers(Long liveStreamId) {
    List<LiveView> liveViews = liveViewRepository.findActiveViewers(liveStreamId);

    if (liveViews.isEmpty()) {
      return List.of();
    }

    // Lấy userIds
    List<Long> userIds = liveViews.stream()
      .map(LiveView::getUserId)
      .toList();

    // Gọi IAM service để lấy thông tin user
    var response = iamClient.getUserInfoBatch(userIds);

    if (response.getData() == null) {
      log.warn("Failed to get user info from IAM service");
      return List.of();
    }

    Map<Long, UserInfo> userMap = response.getData().stream()
      .collect(Collectors.toMap(UserInfo::getId, u -> u));

    // Map sang ViewerResponse
    return liveViews.stream()
      .map(lv -> {
        UserInfo user = userMap.get(lv.getUserId());
        if (user == null) {
          return ViewerResponse.builder()
            .userId(lv.getUserId())
            .username("Unknown")
            .avatarUrl(null)
            .joinedAt(lv.getJoinedAt())
            .build();
        }
        return ViewerResponse.builder()
          .userId(user.getId())
          .username(user.getUsername())
          .avatarUrl(user.getAvatarUrl())
          .joinedAt(lv.getJoinedAt())
          .build();
      })
      .toList();
  }

  private void broadcastViewerJoin(Long liveStreamId, Long userId, LocalDateTime joinedAt) {
    try {
      // Lấy thông tin user
      var userResponse = iamClient.getUserInfo(userId);

      if (userResponse.getData() == null) {
        log.warn("Failed to get user info for userId: {}", userId);
        return;
      }

      UserInfo user = userResponse.getData();

      ViewerResponse viewerResponse = ViewerResponse.builder()
        .userId(user.getId())
        .username(user.getUsername())
        .avatarUrl(user.getAvatarUrl())
        .joinedAt(joinedAt)
        .build();

      ViewerUpdateMessage message = ViewerUpdateMessage.builder()
        .type("JOIN")
        .viewer(viewerResponse)
        .build();

      messagingTemplate.convertAndSend(
        "/topic/viewer-update/" + liveStreamId,
        message
      );

      log.info("Broadcasted JOIN for user {} in livestream {}", userId, liveStreamId);

    } catch (Exception e) {
      log.error("Error broadcasting viewer join", e);
    }
  }

  public void broadcastViewerLeave(Long liveStreamId, Long userId) {
    try {
      ViewerUpdateMessage message = ViewerUpdateMessage.builder()
        .type("LEAVE")
        .userId(userId)
        .build();

      messagingTemplate.convertAndSend(
        "/topic/viewer-update/" + liveStreamId,
        message
      );

      log.info("Broadcasted LEAVE for user {} in livestream {}", userId, liveStreamId);

    } catch (Exception e) {
      log.error("Error broadcasting viewer leave", e);
    }
  }
}
