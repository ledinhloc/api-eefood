package com.eefood.notificationservice.consumer;


import com.eefood.notificationservice.dto.response.OtpResponse;
import com.eefood.notificationservice.enums.OtpType;
import com.eefood.notificationservice.service.OtpEmailService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailConsumer {
    private final ObjectMapper objectMapper;
    private final OtpEmailService otpEmailService;

    @KafkaListener(topics = "notification.email", groupId = "notification-service-group")
    public void handleSendEmail(Map<String, Object> payload) {
        try {
            OtpResponse otpResponse = OtpResponse.builder()
                    .email((String) payload.get("email"))
                    .otpCode((String) payload.get("otpCode"))
                    .otpType(OtpType.valueOf((String) payload.get("otpType")))
                    .build();

            otpEmailService.sendEmail(otpResponse.getEmail(), otpResponse.getOtpCode(), otpResponse.getOtpType());
        } catch (Exception e) {
            log.error("Error while processing Kafka message: {}", e.getMessage(), e);
        }
    }
}
