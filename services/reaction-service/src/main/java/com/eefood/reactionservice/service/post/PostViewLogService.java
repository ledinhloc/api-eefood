package com.eefood.reactionservice.service.post;

import com.eefood.reactionservice.model.Post;
import com.eefood.reactionservice.model.PostViewLog;
import com.eefood.reactionservice.repository.post.PostRepository;
import com.eefood.reactionservice.repository.post.PostViewLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PostViewLogService {
  private final PostViewLogRepository postViewLogRepository;
  private final PostRepository postRepository;

  public List<String> getTopKeywordsFromViewedPosts(Long userId, int limitPost, int limitKeyword, int days) {
    LocalDateTime since = LocalDateTime.now().minusDays(days);

    return postViewLogRepository.findAllByUserIdAndViewedAtAfter(userId, since).stream()
      .sorted((a, b) -> Long.compare(b.getViewDuration(), a.getViewDuration())) // ưu tiên xem lâu
      .limit(limitPost)
      .map(PostViewLog::getPost)
      .distinct()
      .flatMap(post -> {
        List<String> keywords = new ArrayList<>();
        if (post.getRecipeIngredientKeywords() != null)
          keywords.addAll(post.getRecipeIngredientKeywords());
//        if (post.getRecipeCategories() != null)
//          keywords.addAll(post.getRecipeCategories());
//        if (post.getTitle() != null)
//          keywords.add(post.getTitle());
        return keywords.stream();
      })
      .collect(Collectors.groupingBy(k -> k, Collectors.counting()))
      .entrySet().stream()
      .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
      .limit(limitKeyword)
      .map(Map.Entry::getKey)
      .toList();
  }

  public void logView(Long userId, Long postId, Long viewDuration, LocalDateTime viewedAt) {
    Post post = postRepository.findById(postId)
      .orElseThrow(() -> new RuntimeException("Post not found"));

    PostViewLog log = PostViewLog.builder()
      .userId(userId)
      .post(post)
      .viewedAt(viewedAt)
      .viewDuration(viewDuration)
      .build();

    postViewLogRepository.save(log);
  }
}