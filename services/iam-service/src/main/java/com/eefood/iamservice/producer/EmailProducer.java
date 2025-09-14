package com.eefood.iamservice.producer;

import com.eefood.iamservice.dto.request.OtpCreateRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailProducer {
  private final KafkaTemplate<String, Object> kafkaTemplate;
  private static final String EMAIL_TOPIC = "notification.email";

  public void sendEmailProducerEvent(OtpCreateRequest otpCreateRequest) {
    kafkaTemplate.send(EMAIL_TOPIC, otpCreateRequest);
  }
}
