package com.eefood.reactionservice.livestream.service;

import com.eefood.reactionservice.enums.LiveStreamStatus;
import com.eefood.reactionservice.livestream.dto.request.SubtitleWorkerStartRequest;
import com.eefood.reactionservice.livestream.dto.ws.LiveSubtitleMessage;
import com.eefood.reactionservice.livestream.dto.ws.SubtitleSubscriptionRequest;
import com.eefood.reactionservice.livestream.enums.SubtitleLanguage;
import com.eefood.reactionservice.livestream.model.LiveStream;
import com.eefood.reactionservice.livestream.repository.LiveStreamRepository;
import com.eefood.reactionservice.livestream.repository.httpclient.SubtitleWorkerClient;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class LiveSubtitlePreferenceService {

  private final SimpMessagingTemplate messagingTemplate;
  private final LiveStreamRepository liveStreamRepository;
  private final SubtitleWorkerClient subtitleWorkerClient;

  private final Map<String, SubtitleSubscription> subscriptionsBySessionId = new ConcurrentHashMap<>();

  //Đăng ký preference của user
  public void register(Principal principal, String sessionId, SubtitleSubscriptionRequest request) {
    if (principal == null || principal.getName() == null || principal.getName().isBlank()) {
      throw new IllegalArgumentException("WebSocket principal is required");
    }
    if (sessionId == null || sessionId.isBlank()) {
      throw new IllegalArgumentException("WebSocket session id is required");
    }
    if (request == null || request.getLiveStreamId() == null) {
      throw new IllegalArgumentException("Live stream id is required");
    }

    SubtitleLanguage targetLanguage = SubtitleLanguage.fromCode(request.getTargetLanguage());

    SubtitleSubscription previousSubscription = subscriptionsBySessionId.put(
      sessionId,
      SubtitleSubscription.builder()
        .sessionId(sessionId)
        .userId(principal.getName())
        .liveStreamId(request.getLiveStreamId())
        .targetLanguage(targetLanguage)
        .build()
    );
    if (previousSubscription != null) {
      stopWorkerIfNoSubscribers(previousSubscription.getLiveStreamId(), previousSubscription.getTargetLanguage());
    }
    startWorkerIfNeeded(request.getLiveStreamId(), targetLanguage);
  }

  public boolean hasSubscribers(Long liveStreamId, SubtitleLanguage spokenLanguage) {
    if (liveStreamId == null || spokenLanguage == null) {
      return false;
    }
    return subscriptionsBySessionId.values().stream()
      .anyMatch(subscription ->
        subscription.getLiveStreamId().equals(liveStreamId)
          && subscription.getTargetLanguage() == spokenLanguage
      );
  }

  public void unregister(String sessionId) {
    if (sessionId == null || sessionId.isBlank()) {
      return;
    }
    SubtitleSubscription subscription = subscriptionsBySessionId.remove(sessionId);
    if (subscription != null) {
      stopWorkerIfNoSubscribers(subscription.getLiveStreamId(), subscription.getTargetLanguage());
    }
  }

  //Push message qua WebSocket
  public void sendToSubscribers(Long liveStreamId, String spokenLanguageCode, LiveSubtitleMessage message) {
    SubtitleLanguage spokenLanguage = SubtitleLanguage.fromCode(spokenLanguageCode);

    subscriptionsBySessionId.values().stream()
      .filter(subscription -> subscription.getLiveStreamId().equals(liveStreamId))
      .filter(subscription -> subscription.getTargetLanguage() == spokenLanguage)
      .forEach(subscription -> messagingTemplate.convertAndSendToUser(
        subscription.getUserId(),
        "/queue/livestream/subtitles",
        message,
        createHeaders(subscription.getSessionId())
      ));
  }

  //xóa subscription khi socket disconnect
  @EventListener
  public void handleSessionDisconnect(SessionDisconnectEvent event) {
    unregister(event.getSessionId());
  }

  private void startWorkerIfNeeded(Long liveStreamId, SubtitleLanguage targetLanguage) {
    liveStreamRepository.findByIdAndStatus(liveStreamId, LiveStreamStatus.LIVE)
      .filter(liveStream -> liveStream.getSpokenLanguage() == targetLanguage)
      .ifPresent(this::startSubtitleWorker);
  }

  private void stopWorkerIfNoSubscribers(Long liveStreamId, SubtitleLanguage targetLanguage) {
    liveStreamRepository.findByIdAndStatus(liveStreamId, LiveStreamStatus.LIVE)
      .filter(liveStream -> liveStream.getSpokenLanguage() == targetLanguage)
      .filter(liveStream -> !hasSubscribers(liveStreamId, targetLanguage))
      .ifPresent(liveStream -> stopSubtitleWorker(liveStream.getId()));
  }

  private void startSubtitleWorker(LiveStream liveStream) {
    try {
      subtitleWorkerClient.start(
        SubtitleWorkerStartRequest.builder()
          .liveStreamId(liveStream.getId())
          .roomName(liveStream.getRoomName())
          .spokenLanguage(liveStream.getSpokenLanguage().getCode())
          .build()
      );
    } catch (Exception e) {
      log.warn("Cannot start subtitle worker for livestream {}: {}", liveStream.getId(), e.getMessage());
    }
  }

  private void stopSubtitleWorker(Long liveStreamId) {
    try {
      subtitleWorkerClient.stop(Map.of("liveStreamId", liveStreamId));
    } catch (Exception e) {
      log.warn("Cannot stop subtitle worker for livestream {}: {}", liveStreamId, e.getMessage());
    }
  }

  private Map<String, Object> createHeaders(String sessionId) {
    SimpMessageHeaderAccessor headerAccessor = SimpMessageHeaderAccessor.create();
    headerAccessor.setSessionId(sessionId);
    headerAccessor.setLeaveMutable(true);
    return headerAccessor.getMessageHeaders();
  }

  @Value
  @Builder
  private static class SubtitleSubscription {
    String sessionId;
    String userId;
    Long liveStreamId;
    SubtitleLanguage targetLanguage;
  }
}
