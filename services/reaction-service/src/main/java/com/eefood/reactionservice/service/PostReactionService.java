package com.eefood.reactionservice.service;

import com.eefood.reactionservice.dto.request.PostReactionRequest;
import com.eefood.reactionservice.dto.response.PostReactionResponse;
import com.eefood.reactionservice.dto.response.PostResponse;
import com.eefood.reactionservice.model.Post;
import com.eefood.reactionservice.model.PostReaction;
import com.eefood.reactionservice.repository.PostReactionRepository;
import com.eefood.reactionservice.repository.PostRepository;
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

  public PostReactionResponse reactToPost(PostReactionRequest request, Long userId) {
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
        return postReactionMapper.toResponse(postReactionRepository.save(exitingReaction));
      }
    }

    PostReaction newReaction = PostReaction.builder()
      .post(post)
      .userId(userId)
      .reactionType(request.getReactionType())
      .build();

    PostReaction saved = postReactionRepository.save(newReaction);
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
