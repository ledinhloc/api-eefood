package com.eefood.notificationservice.consumer;

import com.eefood.notificationservice.dto.request.NotificationRequest;
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

    @KafkaListener(topics = "notifications.app", groupId = "notification-service-group")
    public void consume(NotificationRequest request) {
        try {
            log.info("Received event from other service: {}", request);
            notificationsService.handleNotificationIncome(request);
        } catch (Exception e) {
            log.error("Error processing notification event: {}", e.getMessage(), e);
        }
    }
}
