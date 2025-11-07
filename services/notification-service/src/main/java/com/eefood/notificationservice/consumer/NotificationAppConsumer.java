package com.eefood.notificationservice.consumer;

import com.eefood.notificationservice.dto.request.NotificationRequest;
import com.eefood.notificationservice.dto.request.UserNotificationResquest;
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

    @KafkaListener(topics = "notifications.app", groupId = "notification-service-group", containerFactory = "notificationKafkaListenerContainerFactory")
    public void consume(NotificationRequest request) {
        try {
            log.info("Received event from other service: {}", request);
            notificationsService.handleNotificationIncome(request);
        } catch (Exception e) {
            log.error("Error processing notification event: {}", e.getMessage(), e);
        }
    }

    @KafkaListener(topics = "register-fcm-token", groupId = "notification-service-group", containerFactory = "userNotificationKafkaListenerContainerFactory")
    public void handleRegisterToken(UserNotificationResquest request) {
        log.info("Received FCM registration from IAM: userId={}, token={}", request.getId(), request.getFcmToken());
        firebaseNotificationService.registerUserToken(request.getId(), request.getFcmToken());
    }
}
