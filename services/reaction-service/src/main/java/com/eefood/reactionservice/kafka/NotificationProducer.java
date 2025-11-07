package com.eefood.reactionservice.kafka;

import com.eefood.reactionservice.dto.request.NotificationRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NotificationProducer {
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private static final String EMAIL_TOPIC = "notifications.app";

    public void sendNotification(NotificationRequest notification) {
        kafkaTemplate.send(EMAIL_TOPIC, notification);
    }
}
