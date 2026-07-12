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
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

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

    //Map.put(key, value) được thiết kế để trả về value cũ của key đó, không trả về value vừa thêm.
    SubtitleSubscription previousSubscription = subscriptionsBySessionId.put(
      sessionId,
      SubtitleSubscription.builder()
        .sessionId(sessionId)
        .userId(principal.getName())
        .liveStreamId(request.getLiveStreamId())
        .targetLanguage(targetLanguage)
        .build()
    );
    //Nếu session này từng đăng ký cái khác, kiểm tra dừng worker cũ
    if (previousSubscription != null) {
      stopWorkerIfNoSubscribers(previousSubscription.getLiveStreamId(), previousSubscription.getTargetLanguage());
    }
    startWorkerIfNeeded(request.getLiveStreamId(), targetLanguage);
  }

  public boolean hasSubscribers(Long liveStreamId, SubtitleLanguage targetLanguage) {
    if (liveStreamId == null || targetLanguage == null) {
      return false;
    }
    return subscriptionsBySessionId.values().stream()
      .anyMatch(subscription ->
        subscription.getLiveStreamId().equals(liveStreamId)
          && subscription.getTargetLanguage() == targetLanguage
      );
  }

  public Set<SubtitleLanguage> getSubscribedTargetLanguages(Long liveStreamId) {
    return subscriptionsBySessionId.values().stream()
      .filter(subscription -> subscription.getLiveStreamId().equals(liveStreamId))
      .map(SubtitleSubscription::getTargetLanguage)
      .collect(Collectors.toSet());
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
  public void sendToSubscribers(Long liveStreamId, String targetLanguageCode, LiveSubtitleMessage message) {
    SubtitleLanguage targetLanguage = SubtitleLanguage.fromCode(targetLanguageCode);

    subscriptionsBySessionId.values().stream()
      .filter(subscription -> subscription.getLiveStreamId().equals(liveStreamId))
      .filter(subscription -> subscription.getTargetLanguage() == targetLanguage)
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
      .filter(liveStream -> supportsTranslation(liveStream.getSpokenLanguage(), targetLanguage))
      .ifPresent(liveStream -> startSubtitleWorker(liveStream, targetLanguage));
  }

  private void stopWorkerIfNoSubscribers(Long liveStreamId, SubtitleLanguage targetLanguage) {
    liveStreamRepository.findByIdAndStatus(liveStreamId, LiveStreamStatus.LIVE)
      .filter(liveStream -> supportsTranslation(liveStream.getSpokenLanguage(), targetLanguage))
      .filter(liveStream -> !hasSubscribers(liveStreamId, targetLanguage))
      .ifPresent(liveStream -> stopSubtitleWorker(liveStream.getId(), targetLanguage));
  }

  private boolean supportsTranslation(
    SubtitleLanguage spokenLanguage,
    SubtitleLanguage targetLanguage
  ) {
    return spokenLanguage == targetLanguage
      || spokenLanguage == SubtitleLanguage.VI && targetLanguage == SubtitleLanguage.EN;
  }

  private void startSubtitleWorker(LiveStream liveStream, SubtitleLanguage targetLanguage) {
    try {
      log.info(
        "Starting subtitle worker: mode={} livestream={} spoken={} target={}",
        liveStream.getSpokenLanguage() == targetLanguage ? "SUBTITLE" : "TRANSLATE",
        liveStream.getId(),
        liveStream.getSpokenLanguage().getCode(),
        targetLanguage.getCode()
      );
      subtitleWorkerClient.start(
        SubtitleWorkerStartRequest.builder()
          .liveStreamId(liveStream.getId())
          .roomName(liveStream.getRoomName())
          .spokenLanguage(liveStream.getSpokenLanguage().getCode())
          .targetLanguage(targetLanguage.getCode())
          .build()
      );
    } catch (Exception e) {
      log.warn("Cannot start subtitle worker for livestream {}: {}", liveStream.getId(), e.getMessage());
    }
  }

  private void stopSubtitleWorker(Long liveStreamId, SubtitleLanguage targetLanguage) {
    try {
      subtitleWorkerClient.stop(Map.of(
        "liveStreamId", liveStreamId,
        "targetLanguage", targetLanguage.getCode()
      ));
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
