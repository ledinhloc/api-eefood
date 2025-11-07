package com.eefood.notificationservice.service;

import com.eefood.notificationservice.dto.request.NotificationRequest;
import com.eefood.notificationservice.dto.response.NotificationResponse;
import com.eefood.notificationservice.model.UserFcmToken;
import com.eefood.notificationservice.repository.UserFcmTokenRepository;
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

@Slf4j
@Service
@RequiredArgsConstructor
public class FirebaseNotificationService {
    private final ConcurrentHashMap<Long, String> userFcmTokens = new ConcurrentHashMap<>();
    private final UserFcmTokenRepository userFcmTokenRepository;

    public void preloadCache() {
        userFcmTokenRepository.findAll()
                .forEach(token -> userFcmTokens.put(token.getUserId(), token.getFcmToken()));
        log.info("Preloaded {} FCM tokens to memory cache", userFcmTokens.size());
    }

    @PostConstruct
    public void init() {
        preloadCache();
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
        Optional<UserFcmToken> userFcmToken = userFcmTokenRepository.findById(userId);
        if (userFcmToken.isPresent()) {
            UserFcmToken userFcmTokenEntity = userFcmToken.get();
            userFcmTokenEntity.setFcmToken(token);
            userFcmTokenRepository.save(userFcmTokenEntity);
        }
        userFcmTokenRepository.save(
                UserFcmToken.builder().userId(userId)
                .fcmToken(token)
                .build());
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
        try {
            Message message = Message.builder()
                    .setToken(token)
                    .setNotification(Notification.builder()
                            .setTitle(response.getTitle())
                            .setBody(response.getBody())
                            .build())
                    .putAllData(Map.of(
                            "title", response.getTitle(),
                            "body", response.getBody(),
                            "type", response.getType(),
                            "path", response.getPath(),
                            "avatarUrl", response.getAvatarUrl(),
                            "postImageUrl", response.getPostImageUrl(),
                            "isRead", String.valueOf(response.isRead())
                    ))
                    .build();

            String messageId = FirebaseMessaging.getInstance().send(message);
            log.info("FCM sent to userId={} messageId={}", userId, messageId);

        } catch (FirebaseMessagingException e) {
            log.error("Failed to send FCM to userId={}, error={}", userId, e.getMessage());
        }
    }

    public void sendBroadcast(NotificationResponse response) {
        if (userFcmTokens.isEmpty()) {
            log.warn("No FCM tokens found for broadcast");
            return;
        }

        List<String> tokens = new ArrayList<>(userFcmTokens.values());
        List<List<String>> batches = partition(tokens, 500); // FCM giới hạn 500 token/batch

        for (List<String> batch : batches) {
            MulticastMessage message = MulticastMessage.builder()
                    .setNotification(Notification.builder()
                            .setTitle(response.getTitle())
                            .setBody(response.getBody())
                            .build())
                    .putAllData(Map.of(
                            "title", response.getTitle(),
                            "body", response.getBody(),
                            "type", response.getType(),
                            "path", response.getPath(),
                            "avatarUrl", response.getAvatarUrl(),
                            "postImageUrl", response.getPostImageUrl(),
                            "isRead", String.valueOf(response.isRead())
                    ))
                    .addAllTokens(batch)
                    .build();

            try {
                BatchResponse result = FirebaseMessaging.getInstance().sendMulticast(message);
                log.info("Broadcast batch sent: {} tokens, success={}, failure={}",
                        batch.size(), result.getSuccessCount(), result.getFailureCount());
            } catch (FirebaseMessagingException e) {
                log.error("Broadcast error in batch of {} tokens: {}", batch.size(), e.getMessage());
            }
        }
    }

    private List<List<String>> partition(List<String> list, int size) {
        List<List<String>> parts = new ArrayList<>();
        for (int i = 0; i < list.size(); i += size) {
            parts.add(list.subList(i, Math.min(list.size(), i + size)));
        }
        return parts;
    }

}
