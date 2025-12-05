package com.eefood.iamservice.kafka;
import com.eefood.common.avro.FirebaseNotificationEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class FirebaseNotificationProducer {
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private static final String FIREBASE_REGISTER_TOPIC = "register-fcm-token";

    public void sendFirebaseRegisterEvent(FirebaseNotificationEvent event) {
        kafkaTemplate.send(FIREBASE_REGISTER_TOPIC, event);
        log.info("Sent Kafka register-fcm-token for user {}", event.getId());
    }
}
