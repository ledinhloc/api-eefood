package com.eefood.reactionservice.livestream.service;

import com.eefood.reactionservice.livestream.dto.ws.LiveSubtitleMessage;
import com.eefood.reactionservice.livestream.dto.ws.SubtitleSubscriptionRequest;
import com.eefood.reactionservice.livestream.enums.SubtitleLanguage;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class LiveSubtitlePreferenceService {

  private final SimpMessagingTemplate messagingTemplate;

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

    subscriptionsBySessionId.put(
      sessionId,
      SubtitleSubscription.builder()
        .sessionId(sessionId)
        .userId(principal.getName())
        .liveStreamId(request.getLiveStreamId())
        .targetLanguage(targetLanguage)
        .build()
    );
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
    subscriptionsBySessionId.remove(event.getSessionId());
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
