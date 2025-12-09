package com.eefood.recipeservice.kafka;

import com.eefood.common.avro.PostApprovalResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ApprovalResultProducer {

  private static final String RESULT_TOPIC = "post.approval.result";

  private final KafkaTemplate<String, Object> kafkaTemplate;

  public void sendResult(PostApprovalResult result) {
    try {
      kafkaTemplate.send(RESULT_TOPIC, result);
      log.debug("Successfully sent approval result to Kafka - PostId: {}",
        result.getPostId());
    } catch (Exception e) {
      log.error("Failed to send approval result - PostId: {}, Error: {}",
        result.getPostId(), e.getMessage());
      throw new RuntimeException("Kafka send failed", e);
    }
  }
}