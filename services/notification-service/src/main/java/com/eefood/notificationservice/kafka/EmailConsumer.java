package com.eefood.notificationservice.kafka;

import com.eefood.common.avro.OtpCreateEvent;
import com.eefood.common.avro.OtpType;
import com.eefood.notificationservice.service.OtpEmailService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailConsumer {
  private final ObjectMapper objectMapper;
  private final OtpEmailService otpEmailService;

  @KafkaListener(topics = "notification.email", groupId = "notification-service-group")
  public void handleSendEmail(OtpCreateEvent event) {
    try {
      log.info("Received OTP event for email: {}, type: {}", event.getEmail(), event.getOtpType());


      // Gọi service gửi email
      String email = event.getEmail().toString();
      String otpCode = event.getOtpCode().toString();
      otpEmailService.sendEmail(email, otpCode,
        convertOtpType(event.getOtpType()));
    } catch (Exception e) {
      log.error("Error while processing Kafka message: {}", e.getMessage(), e);
    }
  }

  // Chuyển đổi enum Avro sang enum trong notification service (nếu cần)
  private com.eefood.notificationservice.enums.OtpType convertOtpType(OtpType otpType) {
    return com.eefood.notificationservice.enums.OtpType.valueOf(otpType.name());
  }
}
