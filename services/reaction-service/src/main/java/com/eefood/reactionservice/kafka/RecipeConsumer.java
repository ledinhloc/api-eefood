package com.eefood.reactionservice.kafka;

import com.eefood.common.avro.NotificationEvent;
import com.eefood.common.avro.RecipeEvent;
import com.eefood.reactionservice.enums.Difficulty;
import com.eefood.reactionservice.enums.PostStatus;
import com.eefood.reactionservice.model.Post;
import com.eefood.reactionservice.repository.post.PostRepository;
import com.eefood.reactionservice.service.post.PostIndexer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecipeConsumer {
  private final PostRepository postRepository;
  private final PostIndexer postIndexer;
  private final PostApprovalProducer postApprovalProducer;
  private final NotificationProducer notificationProducer;

  @KafkaListener(topics = "recipe-update-topic", groupId = "reaction-service-group")
  @Transactional
  public void consume(RecipeEvent event) {
    log.info("Received RecipeEvent: id={}, title={}", event.getId(), event.getTitle());

    Post post = postRepository.findByRecipeIdAndIsDeletedFalse(event.getId());
    if (post == null) {
      log.warn("No post found with recipeId={} - skip update", event.getId());
      return;
    }

    boolean needsReApproval = hasSignificantChanges(post, event);
    updatePostFields(post, event);
    if (needsReApproval) {
      post.setStatus(PostStatus.EDITED_PENDING);
      log.info("Recipe has significant changes - post {} requires re-approval", post.getId());
    }

    postRepository.save(post);
    postIndexer.saveOrUpdatePost(post);

    if (needsReApproval) {
      postApprovalProducer.sendApprovalRequest(post);

      //gui thong bao
      NotificationEvent notification = NotificationEvent.newBuilder()
        .setTitle("Bài đăng đang chờ duyệt lại!")
        .setBody("Hệ thống đang xem xét " + post.getTitle() + " của bạn.")
        .setPath("/recipe-crud/"+post.getId())
        .setAvatarUrl("")
        .setPostImageUrl("")
        .setType("SYSTEM")
        .setUserId(post.getUserId())
        .build();
      notificationProducer.sendNotification(notification);

      log.info("Sent re-approval request for post {}", post.getId());
    } else {
      log.info("Updated post {} without re-approval (minor changes only)", post.getId());
    }
  }

  private boolean hasSignificantChanges(Post post, RecipeEvent event) {
    return isTitleChanged(post, event) ||
      isDescriptionChanged(post, event) ||
      isImageChanged(post, event) ||
      areIngredientsChanged(post, event) ||
      areCategoriesChanged(post, event);
  }

  private boolean isTitleChanged(Post post, RecipeEvent event) {
    String newTitle = toString(event.getTitle());
    return !Objects.equals(post.getTitle(), newTitle);
  }

  private boolean isDescriptionChanged(Post post, RecipeEvent event) {
    String newDesc = toString(event.getDescription());
    return !Objects.equals(post.getDescription(), newDesc);
  }

  private boolean isImageChanged(Post post, RecipeEvent event) {
    String newImage = toString(event.getImageUrl());
    return !Objects.equals(post.getImageUrl(), newImage);
  }

  private boolean areIngredientsChanged(Post post, RecipeEvent event) {
    Set<String> oldIngredients = post.getRecipeIngredientKeywords();
    Set<String> newIngredients = toStringSet(event.getIngredientKeywords());
    return !oldIngredients.equals(newIngredients);
  }

  private boolean areCategoriesChanged(Post post, RecipeEvent event) {
    Set<String> oldCategories = post.getRecipeCategories();
    Set<String> newCategories = toStringSet(event.getCategories());
    return !oldCategories.equals(newCategories);
  }

  private void updatePostFields(Post post, RecipeEvent event) {
    post.setTitle(toString(event.getTitle()));
    post.setDescription(toString(event.getDescription()));
    post.setRegion(toString(event.getRegion()));
    post.setImageUrl(toString(event.getImageUrl()));
    post.setPrepTime(event.getPrepTime());
    post.setCookTime(event.getCookTime());
    post.setDifficulty(parseDifficulty(event.getDifficulty()));
    post.setRecipeCategories(toStringSet(event.getCategories()));
    post.setRecipeIngredientKeywords(toStringSet(event.getIngredientKeywords()));
  }

  private String toString(CharSequence value) {
    return value == null ? null : value.toString();
  }

  private Set<String> toStringSet(List<CharSequence> values) {
    if (values == null) {
      return new HashSet<>();
    }
    return values.stream()
      .map(CharSequence::toString)
      .collect(Collectors.toSet());
  }

  private Difficulty parseDifficulty(CharSequence difficulty) {
    if (difficulty == null) {
      return null;
    }
    try {
      return Difficulty.valueOf(difficulty.toString());
    } catch (IllegalArgumentException e) {
      log.warn("Invalid difficulty value: {}", difficulty);
      return null;
    }
  }
}