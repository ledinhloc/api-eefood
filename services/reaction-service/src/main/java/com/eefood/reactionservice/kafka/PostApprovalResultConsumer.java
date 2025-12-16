package com.eefood.reactionservice.kafka;

import com.eefood.common.avro.NotificationEvent;
import com.eefood.common.avro.PostApprovalResult;
import com.eefood.reactionservice.enums.PostStatus;
import com.eefood.reactionservice.model.ApprovePost;
import com.eefood.reactionservice.model.Post;
import com.eefood.reactionservice.repository.post.PostRepository;
import com.eefood.reactionservice.service.ApprovePostService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

@Service
@RequiredArgsConstructor
@Slf4j
public class PostApprovalResultConsumer {

  private final PostRepository postRepository;
  private final NotificationProducer notificationProducer;
  private final ApprovePostService approvePostService;

  @KafkaListener(topics = "post.approval.result", groupId = "reaction-service")
  @Transactional
  public void handleApprovalResult(PostApprovalResult result) {
    log.info("Received approval result - PostId: {}, Status: {}",
      result.getPostId(), result.getStatus());

    try {
      Post post = fetchPost(result.getPostId());

      if (post == null) {
        log.error("Post not found - PostId: {}", result.getPostId());
        return;
      }

      // luu ket qua duyet
      ApprovePost approvePost = mapToApprovePost(post, result);
      approvePostService.save(approvePost);
      updatePostStatus(post, result);
      sendNotificationToUser(post, result);

      log.info("Successfully processed approval result - PostId: {}, Status: {}",
        post.getId(), post.getStatus());

    } catch (Exception e) {
      log.error("Error processing approval result - PostId: {}, Error: {}",
        result.getPostId(), e.getMessage(), e);
    }
  }

  private ApprovePost mapToApprovePost(Post post, PostApprovalResult result) {
    return ApprovePost.builder()
      .post(post)
      .userId(result.getUserId())
      .recipeId(result.getRecipeId())
      .status(String.valueOf(result.getStatus()))
      .summary(String.valueOf(result.getSummary()))
      .totalScore(result.getTotalScore())
      .recipeCompleteness(result.getRecipeCompleteness())
      .ingredientSafety(result.getIngredientSafety())
      .stepClarity(result.getStepClarity())
      .contentAppropriate(result.getContentAppropriate())
      .contentRelevance(result.getContentRelevance())
      .mediaQuality(result.getMediaQuality())
      .completenessNote(String.valueOf(result.getCompletenessNote()))
      .safetyNote(String.valueOf(result.getSafetyNote()))
      .clarityNote(String.valueOf(result.getClarityNote()))
      .appropriatenessNote(String.valueOf(result.getAppropriatenessNote()))
      .relevanceNote(String.valueOf(result.getRelevanceNote()))
      .mediaQualityNote(String.valueOf(result.getMediaQualityNote()))
      .build();
  }

  private Post fetchPost(Long postId) {
    Post post = postRepository.findByIdAndIsDeletedFalse(postId);

    if (post == null) {
      log.warn("Post not found or already deleted - PostId: {}", postId);
    }

    return post;
  }

  private void updatePostStatus(Post post, PostApprovalResult result) {
    PostStatus newStatus = parseStatus(String.valueOf(result.getStatus()));
    PostStatus oldStatus = post.getStatus();

    post.setStatus(newStatus);
    post.setApprovedBy("AI");
    postRepository.save(post);

    log.info("Updated post status - PostId: {}, OldStatus: {}, NewStatus: {}",
      post.getId(), oldStatus, newStatus);
  }

  private PostStatus parseStatus(String status) {
    try {
      return PostStatus.valueOf(status.toUpperCase());
    } catch (IllegalArgumentException e) {
      log.error("Invalid status received: {}, defaulting to PENDING", status);
      return PostStatus.PENDING;
    }
  }

  private void sendNotificationToUser(Post post, PostApprovalResult result) {
    PostStatus status = parseStatus(String.valueOf(result.getStatus()));

    if (status == PostStatus.PENDING) {
      log.debug("Skipping notification for PENDING status - PostId: {}", post.getId());
      return;
    }

    NotificationEvent notification = buildNotification(post, result, status);
    notificationProducer.sendNotification(notification);

    log.info("Notification sent to user - UserId: {}, PostId: {}, Status: {}",
      post.getUserId(), post.getId(), status);
  }

  private NotificationEvent buildNotification(Post post, PostApprovalResult result, PostStatus status) {
    String title = buildNotificationTitle(status);
    String body = buildNotificationBody(post, result, status);
    String imageUrl = getImageUrl(post, result);

    return NotificationEvent.newBuilder()
      .setTitle(title)
      .setBody(body)
      .setPath(buildPath(post, result, status))
      .setAvatarUrl("")
      .setPostImageUrl(imageUrl)
      .setType("SYSTEM")
      .setUserId(post.getUserId())
      .build();
  }
  private String buildPath(Post post, PostApprovalResult result, PostStatus status) {
//    if (status != PostStatus.APPROVED) {
//      return "/posts/" + post.getId();
//    }

    try {
      String recipeName = post.getTitle() ;
      String message = result.getSummary() != null ? String.valueOf(result.getSummary()) : "Chúc mừng! Công thức đã được phê duyệt.";
      String imageUrl = result.getRecipeImageUrl() != null ? String.valueOf(result.getRecipeImageUrl()) : post.getImageUrl();
      String approvedAt = Instant.ofEpochMilli(result.getProcessedAt()).toString();

      return String.format("/recipe-approve?recipeId=%d&recipeName=%s&message=%s&imageUrl=%s&approvedAt=%s",
        post.getRecipeId(),
        URLEncoder.encode(recipeName, StandardCharsets.UTF_8),
        URLEncoder.encode(message, StandardCharsets.UTF_8),
        URLEncoder.encode(imageUrl != null ? imageUrl : "", StandardCharsets.UTF_8),
        URLEncoder.encode(approvedAt, StandardCharsets.UTF_8)
      );
    } catch (Exception e) {
      return "/posts/" + post.getRecipeId();
    }
  }

  private String buildNotificationTitle(PostStatus status) {
    return switch (status) {
      case APPROVED -> "Bài đăng đã được duyệt";
      case REJECTED -> "Bài đăng bị từ chối";
      default -> "Cập nhật trạng thái bài đăng";
    };
  }

  private String buildNotificationBody(Post post, PostApprovalResult result, PostStatus status) {
    String recipeTitle = result.getRecipeTitle() != null
      ? String.valueOf(result.getRecipeTitle())
      : post.getTitle();

    return switch (status) {
      case APPROVED -> String.format(
        "Công thức \"%s\" của bạn đã được phê duyệt và hiển thị công khai!",
        recipeTitle
      );
      case REJECTED -> String.format(
        "Công thức \"%s\" chưa được duyệt. Lý do: %s",
        recipeTitle,
        result.getSummary() != null ? result.getSummary() : "Không rõ lý do"
      );
      default -> String.format(
        "Công thức \"%s\" đang được xem xét.",
        recipeTitle
      );
    };
  }

  private String getImageUrl(Post post, PostApprovalResult result) {
    if (result.getRecipeImageUrl() != null && !result.getRecipeImageUrl().isEmpty()) {
      return String.valueOf(result.getRecipeImageUrl());
    }
    return post.getImageUrl() != null ? post.getImageUrl() : "";
  }
}