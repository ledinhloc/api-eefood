package com.eefood.notificationservice.service;
import com.eefood.notificationservice.dto.request.NotificationRequest;
import com.eefood.notificationservice.dto.request.UserNotificationResquest;
import com.eefood.notificationservice.dto.response.NotificationResponse;
import com.eefood.notificationservice.dto.response.ResponseData;
import com.eefood.notificationservice.model.UserFcmToken;
import com.eefood.notificationservice.repository.UserFcmTokenRepository;
import com.eefood.notificationservice.repository.httpclient.IamClient;
import com.google.firebase.messaging.*;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FirebaseNotificationService {
    private final ConcurrentHashMap<Long, String> userFcmTokens = new ConcurrentHashMap<>();
    private final UserFcmTokenRepository userFcmTokenRepository;
    private final IamClient iamClient;

    @PostConstruct
    public void init() {
        preloadCache();
    }

    public void preloadCache() {
        userFcmTokenRepository.findAll()
                .forEach(token -> userFcmTokens.put(token.getUserId(), token.getFcmToken()));
        log.info("Preloaded {} FCM tokens to memory cache", userFcmTokens.size());
    }

    public void registerUserToken(Long userId, String token) {
        if (userId == null) {
            log.warn("Cannot register token: userId is null or empty");
            return;
        }
        if (token == null || token.trim().isEmpty()) {
            log.warn("Cannot register token for userId {}: fcmToken is null or empty", userId);
            return;
        }
        userFcmTokens.put(userId, token);
        Optional<UserFcmToken> existingToken = userFcmTokenRepository.findById(userId);
        if (existingToken.isPresent()) {
            UserFcmToken userFcmTokenEntity = existingToken.get();
            userFcmTokenEntity.setFcmToken(token);
            userFcmTokenRepository.save(userFcmTokenEntity);
        } else {
            userFcmTokenRepository.save(
                    UserFcmToken.builder()
                            .userId(userId)
                            .fcmToken(token)
                            .build());
        }
        log.info("Registered FCM token for user {}: {}", userId, token);
    }

    public void unregisterUserToken(Long userId) {
        userFcmTokens.remove(userId);
        userFcmTokenRepository.deleteById((userId));
        log.info("Unregistered FCM token for user {}", userId);
    }

    public void sendNotificationToUser(Long userId, NotificationResponse response) {
        String token = userFcmTokens.get(userId);
        if (token == null) {
            log.warn("No FCM token for user {}, skipping FCM push", userId);
            return;
        }
        sendSingleNotification(userId, token, response);
    }

    public void sendNotificationToListUser(List<Long> userIds, NotificationResponse response) {
        if (userIds == null || userIds.isEmpty()) {
            log.warn("UserId list is empty, skip sending group notification");
            return;
        }

        for (Long userId : userIds) {
            String token = userFcmTokens.get(userId);
            if (token != null && !token.isBlank()) {
                sendSingleNotification(userId, token, response);
            } else {
                log.warn("No FCM token for user {}, skipping", userId);
            }
        }
    }

    public void sendBroadcast(NotificationResponse response) {
        if (userFcmTokens.isEmpty()) {
            log.warn("No FCM tokens found for broadcast");
            return;
        }

        List<String> tokens = new ArrayList<>(userFcmTokens.values());
        sendMulticastNotification(tokens, response, "Broadcast");
    }

    // Hàm chung để gửi notification đơn lẻ
    private void sendSingleNotification(Long userId, String token, NotificationResponse response) {
        try {
            Message message = buildMessage(token, response);
            String messageId = FirebaseMessaging.getInstance().send(message);
            log.info("FCM sent to userId={} messageId={}", userId, messageId);
        } catch (FirebaseMessagingException e) {
            log.error("Failed to send FCM to userId={}, error={}", userId, e.getMessage());
        }
    }

    // Hàm chung để gửi multicast notification
    private void sendMulticastNotification(List<String> tokens, NotificationResponse response, String type) {
        List<List<String>> batches = partition(tokens, 500);

        for (List<String> batch : batches) {
            MulticastMessage message = buildMulticastMessage(batch, response);
            try {
                BatchResponse result = FirebaseMessaging.getInstance().sendEachForMulticast(message);
                log.info("{} batch sent: {} tokens, success={}, failure={}",
                        type, batch.size(), result.getSuccessCount(), result.getFailureCount());
            } catch (FirebaseMessagingException e) {
                log.error("{} error in batch of {} tokens: {}", type, batch.size(), e.getMessage());
            }
        }
    }

    // Hàm chung để build Message
    private Message buildMessage(String token, NotificationResponse response) {
        return Message.builder()
                .setToken(token)
                .setNotification(buildNotification(response))
                .putAllData(buildDataMap(response))
                .build();
    }

    // Hàm chung để build MulticastMessage
    private MulticastMessage buildMulticastMessage(List<String> tokens, NotificationResponse response) {
        return MulticastMessage.builder()
                .setNotification(buildNotification(response))
                .putAllData(buildDataMap(response))
                .addAllTokens(tokens)
                .build();
    }

    // Hàm chung để build Notification
    private Notification buildNotification(NotificationResponse response) {
        return Notification.builder()
                .setTitle(response.getTitle())
                .setBody(response.getBody())
                .build();
    }

    // Hàm chung để build Data Map
    private Map<String, String> buildDataMap(NotificationResponse response) {
        return Map.of(
                "title", response.getTitle(),
                "body", response.getBody(),
                "type", response.getType(),
                "path", response.getPath(),
                "avatarUrl", response.getAvatarUrl(),
                "postImageUrl", response.getPostImageUrl(),
                "isRead", String.valueOf(response.isRead())
        );
    }

    private List<List<String>> partition(List<String> list, int size) {
        List<List<String>> parts = new ArrayList<>();
        for (int i = 0; i < list.size(); i += size) {
            parts.add(list.subList(i, Math.min(list.size(), i + size)));
        }
        return parts;
    }
}