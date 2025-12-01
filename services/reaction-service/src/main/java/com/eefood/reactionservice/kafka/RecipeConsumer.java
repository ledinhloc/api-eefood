package com.eefood.reactionservice.kafka;

import com.eefood.common.avro.RecipeEvent;
import com.eefood.reactionservice.model.Post;
import com.eefood.reactionservice.repository.post.PostRepository;
import com.eefood.reactionservice.service.post.PostIndexer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.stream.Collectors;


@Slf4j
@Service
@RequiredArgsConstructor
public class RecipeConsumer {
  private final PostRepository postRepository;
  private final PostIndexer postIndexer;

  /**
   * Lắng nghe event từ topic recipe-update-topic
   */
  @KafkaListener(topics = "recipe-update-topic", groupId = "reaction-service-group")
  @Transactional
  public void consume(RecipeEvent event) {
    log.info(" Received RecipeEvent: id={}, title={}", event.getId(), event.getTitle());

    Post post = postRepository.findByRecipeIdAndIsDeletedFalse(event.getId());
    if (post == null) {
      log.warn("No post found with recipeId={} → skip update", event.getId());
      return;
    }

    // cập nhật thông tin từ recipe
    post.setTitle(event.getTitle() == null ? null : event.getTitle().toString());
    post.setDescription(event.getDescription() == null ? null : event.getDescription().toString());
    post.setRegion(event.getRegion() == null ? null : event.getRegion().toString());
    post.setImageUrl(event.getImageUrl() == null ? null : event.getImageUrl().toString());
    post.setPrepTime(event.getPrepTime());
    post.setCookTime(event.getCookTime());
    if (event.getDifficulty() != null) {
      try {
        String diff = event.getDifficulty().toString();
        post.setDifficulty(Enum.valueOf(com.eefood.reactionservice.enums.Difficulty.class, diff));
      } catch (IllegalArgumentException e) {
        log.warn("Invalid difficulty value: {}", event.getDifficulty());
      }
    }

    // categories & ingredients
    post.setRecipeCategories(
      event.getCategories() == null
        ? new HashSet<>()
        : event.getCategories().stream()
        .map(CharSequence::toString)
        .collect(Collectors.toCollection(HashSet::new))
    );

    post.setRecipeIngredientKeywords(
      event.getIngredientKeywords() == null
        ? new HashSet<>()
        : event.getIngredientKeywords().stream()
        .map(CharSequence::toString)
        .collect(Collectors.toCollection(HashSet::new))
    );

    postRepository.save(post);
    postIndexer.saveOrUpdatePost(post);
    log.info("Updated post for recipeId={} successfully", event.getId());
  }
}
