package com.eefood.reactionservice.controller;

import com.eefood.reactionservice.service.PostViewLogService;
import com.eefood.reactionservice.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/v1/posts/view-details")
@RequiredArgsConstructor
public class PostViewLogController {
  private final PostViewLogService postViewLogService;
  private final SecurityUtil securityUtil;

  @PostMapping("")
  public ResponseEntity<String> logPostView(
    @RequestParam Long postId,
    @RequestParam Long viewDuration,
    @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime viewedAt
  ) {
    postViewLogService.logView(securityUtil.getCurrentUserId(), postId, viewDuration, viewedAt);
    return ResponseEntity.ok("View log recorded successfully");
  }
}
