package com.eefood.iamservice.producer;

import com.eefood.iamservice.dto.request.OtpCreateRequest;
import com.eefood.iamservice.dto.response.UserNotificationResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class FirebaseNotificationProducer {
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private static final String EMAIL_TOPIC = "register-fcm-token";

    public void sendEmailProducerEvent(UserNotificationResponse response) {
        kafkaTemplate.send(EMAIL_TOPIC, response);
        log.info("Sent Kafka register-fcm-token for user {}", response.getId());
    }
}
