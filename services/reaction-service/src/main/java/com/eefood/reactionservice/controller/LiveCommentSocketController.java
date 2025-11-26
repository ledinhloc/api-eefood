package com.eefood.reactionservice.controller;

import com.eefood.reactionservice.service.livestream.LiveCommentService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class LiveCommentSocketController {
  private final LiveCommentService commentService;

  @MessageMapping("/live/comment")
  public void sendComment(Long liveStreamId, Long userId, String message) {
    commentService.addComment(
      liveStreamId,
      userId,
      message);
  }
}
