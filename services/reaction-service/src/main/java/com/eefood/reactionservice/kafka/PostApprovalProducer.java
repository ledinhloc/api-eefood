package com.eefood.reactionservice.kafka;

import com.eefood.common.avro.PostApprovalRequest;
import com.eefood.reactionservice.model.Post;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class PostApprovalProducer {

  private final KafkaTemplate<String, Object> kafkaTemplate;
  private static final String APPROVAL_TOPIC = "post.approval.request";

  public void sendApprovalRequest(Post post) {
    PostApprovalRequest request = PostApprovalRequest.newBuilder()
      .setPostId(post.getId())
      .setRecipeId(post.getRecipeId())
      .setUserId(post.getUserId())
      .setContent(post.getContent())
      .setCreatedAt(System.currentTimeMillis())
      .build();

    kafkaTemplate.send(APPROVAL_TOPIC, request);
    log.info("Sent approval request for postId={}, recipeId={}",
      post.getId(), post.getRecipeId());
  }
}
