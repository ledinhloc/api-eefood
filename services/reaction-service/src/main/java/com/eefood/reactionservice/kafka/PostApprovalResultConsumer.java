package com.eefood.reactionservice.kafka;

import com.eefood.common.avro.NotificationEvent;
import com.eefood.common.avro.PostApprovalResult;
import com.eefood.reactionservice.enums.PostStatus;
import com.eefood.reactionservice.model.Post;
import com.eefood.reactionservice.repository.post.PostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PostApprovalResultConsumer {

  private final PostRepository postRepository;
  private final NotificationProducer notificationProducer;

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

      updatePostStatus(post, result);
      sendNotificationToUser(post, result);

      log.info("Successfully processed approval result - PostId: {}, Status: {}",
        post.getId(), post.getStatus());

    } catch (Exception e) {
      log.error("Error processing approval result - PostId: {}, Error: {}",
        result.getPostId(), e.getMessage(), e);
    }
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
      .setPath("/posts/" + post.getId())
      .setAvatarUrl("")
      .setPostImageUrl(imageUrl)
      .setType("SYSTEM")
      .setUserId(post.getUserId())
      .build();
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
        result.getReason() != null ? result.getReason() : "Không rõ lý do"
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