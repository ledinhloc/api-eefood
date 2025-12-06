package com.eefood.recipeservice.kafka;

import com.eefood.common.avro.PostApprovalRequest;
import com.eefood.common.avro.PostApprovalResult;
import com.eefood.recipeservice.dto.response.ModerationResult;
import com.eefood.recipeservice.service.RecipeModerationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class RecipeApprovalConsumer {

  private final RecipeModerationService moderationService;
  private final ApprovalResultProducer resultProducer;

  @KafkaListener(topics = "post.approval.request", groupId = "recipe-service")
  public void handleApprovalRequest(PostApprovalRequest request) {
    log.info("Received approval request - PostId: {}, RecipeId: {}",
      request.getPostId(), request.getRecipeId());

    try {
      ModerationResult moderationResult = moderationService.moderateRecipe(
        request.getRecipeId(),
        String.valueOf(request.getContent())
      );

      sendModerationResult(request, moderationResult);

    } catch (Exception e) {
      log.error("Error processing approval request - PostId: {}, Error: {}",
        request.getPostId(), e.getMessage(), e);
      sendRejectionResult(request, "Processing error: " + e.getMessage());
    }
  }

  private void sendModerationResult(PostApprovalRequest request,
                                    ModerationResult moderationResult) {
    PostApprovalResult result = PostApprovalResult.newBuilder()
      .setPostId(request.getPostId())
      .setRecipeId(request.getRecipeId())
      .setUserId(request.getUserId())
      .setStatus(moderationResult.getStatus().name())
      .setReason(moderationResult.getReason())
      .setConfidence(moderationResult.getConfidence())
      .setProcessedAt(System.currentTimeMillis())
      .setRecipeTitle(null) // Will be set if needed
      .setRecipeImageUrl(null)
      .build();

    resultProducer.sendResult(result);
    log.info("Sent moderation result - PostId: {}, Status: {}",
      request.getPostId(), moderationResult.getStatus());
  }

  private void sendRejectionResult(PostApprovalRequest request, String reason) {
    PostApprovalResult result = PostApprovalResult.newBuilder()
      .setPostId(request.getPostId())
      .setRecipeId(request.getRecipeId())
      .setUserId(request.getUserId())
      .setStatus("REJECTED")
      .setReason(reason)
      .setConfidence(1.0)
      .setProcessedAt(System.currentTimeMillis())
      .build();

    resultProducer.sendResult(result);
  }
}