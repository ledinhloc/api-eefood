package com.eefood.reactionservice.controller;

import com.eefood.reactionservice.dto.response.ResponseData;
import com.eefood.reactionservice.service.post.PostViewLogService;
import com.eefood.reactionservice.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/post-views")
@RequiredArgsConstructor
public class PostViewLogController {
  private final PostViewLogService postViewLogService;
  private final SecurityUtil securityUtil;

  @GetMapping
  public ResponseData<List<String>> getKeyword(
    @RequestParam Long userId,
    @RequestParam int limitPost,
    @RequestParam int limitKeyword,
    @RequestParam int days
  ){
     List<String> viewedPostKeywords = postViewLogService.getTopKeywordsFromViewedPosts(userId, limitPost, limitKeyword, days);

    return new ResponseData<>(HttpStatus.OK.value(), "get success", viewedPostKeywords);
  }

  @PostMapping
  public ResponseData<Void> logPostView(
    @RequestParam Long postId,
    @RequestParam Long viewDuration,
    @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime viewedAt
  ) {
    postViewLogService.logView(securityUtil.getCurrentUserId(), postId, viewDuration, viewedAt);
    return new ResponseData<>(HttpStatus.OK.value(), "save log successfully");
  }
}
