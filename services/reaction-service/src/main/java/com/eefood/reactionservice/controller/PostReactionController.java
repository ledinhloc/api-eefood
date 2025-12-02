package com.eefood.reactionservice.controller;

import com.eefood.reactionservice.dto.request.PostReactionRequest;
import com.eefood.reactionservice.dto.response.PostReactionResponse;
import com.eefood.reactionservice.dto.response.ResponseData;
import com.eefood.reactionservice.service.post.PostReactionService;
import com.eefood.reactionservice.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/post-reactions")
@RequiredArgsConstructor
public class PostReactionController {
  private final PostReactionService postReactionService;
  private final SecurityUtil securityUtil;

  @PostMapping
  public ResponseData<PostReactionResponse> reactToPost(@RequestBody PostReactionRequest request) {
    Long userId = securityUtil.getCurrentUserId();
    PostReactionResponse response = postReactionService.reactToPost(request, userId);
    return new ResponseData<>(HttpStatus.OK.value(), "Success", response);
  }

  @DeleteMapping("/{postId}")
  public ResponseData<Void> removeReaction(
    @PathVariable Long postId
  ){
    Long userId = securityUtil.getCurrentUserId();
    postReactionService.removeReaction(postId, userId);
    return new ResponseData<>(HttpStatus.OK.value(), "Delete Success");
  }

  @GetMapping("/{postId}")
  public ResponseData<List<PostReactionResponse>> getReactionsByPost(@PathVariable Long postId){
    return new ResponseData<>(HttpStatus.OK.value(), "Get Success", postReactionService.getReactionsByPost(postId));
  }
}
