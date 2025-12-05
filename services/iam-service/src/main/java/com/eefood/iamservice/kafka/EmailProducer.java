package com.eefood.iamservice.kafka;

import com.eefood.common.avro.OtpCreateEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailProducer {
  private final KafkaTemplate<String, Object> kafkaTemplate;
  private static final String EMAIL_TOPIC = "notification.email";

  public void sendEmailProducerEvent(OtpCreateEvent otpCreateRequest) {
    kafkaTemplate.send(EMAIL_TOPIC, otpCreateRequest);
  }
}
