package com.eefood.notificationservice.kafka;

import com.eefood.common.avro.FirebaseNotificationEvent;
import com.eefood.common.avro.NotificationEvent;
import com.eefood.notificationservice.dto.request.NotificationRequest;
import com.eefood.notificationservice.service.FirebaseNotificationService;
import com.eefood.notificationservice.service.NotificationsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationAppConsumer {
    private final NotificationsService notificationsService;
    private final FirebaseNotificationService firebaseNotificationService;

    @KafkaListener(topics = "notifications.app", groupId = "notification-service-group")
    public void consume(NotificationEvent event) {
        try {
            log.info("Received NotificationEvent from Kafka: {}", event);

            NotificationRequest request = NotificationRequest.builder()
                    .title(event.getTitle().toString())
                    .body(event.getBody().toString())
                    .path(event.getPath().toString())
                    .avatarUrl(event.getAvatarUrl() != null ? event.getAvatarUrl().toString() : null)
                    .postImageUrl(event.getPostImageUrl() != null ? event.getPostImageUrl().toString() : null)
                    .type(event.getType().toString())
                    .userId(event.getUserId() != null ? (Long) event.getUserId() : null)
                    .build();

            notificationsService.handleNotificationIncome(request);
        } catch (Exception e) {
            log.error("Error processing notification event: {}", e.getMessage(), e);
        }
    }

    @KafkaListener(topics = "register-fcm-token", groupId = "notification-service-group")
    public void consumeRegisterToken(FirebaseNotificationEvent event) {
        try {
            log.info("Received RegisterFcmTokenEvent: userId={}, token={}", event.getId(), event.getFcmToken());
            firebaseNotificationService.registerUserToken(
                    event.getId(),
                    event.getFcmToken().toString()
            );

        }
        catch (Exception e) {
            log.error("Error processing RegisterFcmTokenEvent", e);
        }
    }

    @KafkaListener(topics = "unregister-fcm-token", groupId = "notification-service-group")
    public void consumeUnregisterToken(FirebaseNotificationEvent event) {
        try {
            log.info("Received UnregisterFcmTokenEvent: userId={}, token={}", event.getId(), event.getFcmToken());
            firebaseNotificationService.unregisterUserToken(event.getId());

        }
        catch (Exception e) {
            log.error("Error processing RegisterFcmTokenEvent", e);
        }
    }
}