package com.eefood.reactionservice.service;

import com.eefood.reactionservice.dto.request.PostReactionRequest;
import com.eefood.reactionservice.dto.response.PostReactionResponse;
import com.eefood.reactionservice.dto.response.UserInfo;
import com.eefood.reactionservice.mapper.PostReactionMapper;
import com.eefood.reactionservice.model.Post;
import com.eefood.reactionservice.model.PostReaction;
import com.eefood.reactionservice.repository.PostReactionRepository;
import com.eefood.reactionservice.repository.PostRepository;
import com.eefood.reactionservice.repository.httpclient.IamClient;
import com.eefood.reactionservice.util.NotificationUtils;
import com.eefood.reactionservice.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PostReactionService {
  private final PostRepository postRepository;
  private final PostReactionRepository postReactionRepository;
  private final PostReactionMapper postReactionMapper;
  private final NotificationUtils notificationUtils;
  private final SecurityUtil securityUtil;
  private final IamClient iamClient;


  public PostReactionResponse reactToPost(PostReactionRequest request, Long userId) {
    UserInfo userInfo = iamClient.getUserInfo(userId).getData();
    Post post =
        postRepository
            .findById(request.getPostId())
            .orElseThrow(() -> new RuntimeException());

    PostReaction exitingReaction = postReactionRepository
      .findByPostIdAndUserId(request.getPostId(), userId)
      .orElse(null);

    if(exitingReaction != null) {
      //Nếu user chọn lại cùng loại reaction thì gỡ
      if(exitingReaction.getReactionType() == request.getReactionType()) {
        postReactionRepository.delete(exitingReaction);
        return null;
      }else {
        exitingReaction.setReactionType(request.getReactionType());
        if (!userId.equals(post.getUserId())) {
          notificationUtils.sendReactionNotification(
                  post.getUserId(),
                  userInfo.getUsername(),
                  exitingReaction.getReactionType(),
                  userInfo.getAvatarUrl(),
                  true,
                  "/posts/" + post.getId(),
                  post.getImageUrl());
        }
        return postReactionMapper.toResponse(postReactionRepository.save(exitingReaction));
      }
    }

    PostReaction newReaction = PostReaction.builder()
      .post(post)
      .userId(userId)
      .reactionType(request.getReactionType())
      .build();

    PostReaction saved = postReactionRepository.save(newReaction);

    if (!userId.equals(post.getUserId())) {
      notificationUtils.sendReactionNotification(
              post.getUserId(),
              userInfo.getUsername(),
              exitingReaction.getReactionType(),
              userInfo.getAvatarUrl(),
              true,
              "/posts/" + post.getId(),
              post.getImageUrl());
    }
    return postReactionMapper.toResponse(saved);
  }

  public void removeReaction(Long postId, Long userId) {
    postReactionRepository.findByPostIdAndUserId(postId, userId)
      .ifPresent(postReactionRepository::delete);
  }

  public List<PostReactionResponse> getReactionsByPost(Long postId) {
    return postReactionRepository.findAll().stream()
      .filter(r -> r.getPost().getId().equals(postId))
      .map(postReactionMapper::toResponse)
      .collect(Collectors.toList());
  }
}
