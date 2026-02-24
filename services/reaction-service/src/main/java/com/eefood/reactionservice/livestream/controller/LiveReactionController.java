package com.eefood.reactionservice.livestream.controller;

import com.eefood.reactionservice.livestream.dto.response.LiveReactionResponse;
import com.eefood.reactionservice.dto.response.ResponseData;
import com.eefood.reactionservice.enums.FoodEmotion;
import com.eefood.reactionservice.livestream.service.LiveReactionService;
import com.eefood.reactionservice.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/livestreams")
@RequiredArgsConstructor
public class LiveReactionController {
  private final LiveReactionService liveReactionService;
  private final SecurityUtil securityUtil;

  @PostMapping("/{liveId}/reactions")
  public ResponseData<LiveReactionResponse> create(@PathVariable Long liveId,@RequestParam FoodEmotion emotion) {
    Long userId = securityUtil.getCurrentUserId();
    return new ResponseData<>(HttpStatus.OK.value(), "success",
      liveReactionService.createReaction(userId, liveId, emotion));
  }

  @GetMapping("/{liveId}/reactions")
  public ResponseData<List<LiveReactionResponse>> getByStream(@PathVariable Long liveId) {
    return new ResponseData<>(HttpStatus.OK.value(), "success",
      liveReactionService.getReactionsByStream(liveId));
  }
}
