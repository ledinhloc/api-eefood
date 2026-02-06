package com.eefood.reactionservice.livestream.controller;

import com.eefood.reactionservice.livestream.dto.response.LiveStreamResponse;
import com.eefood.reactionservice.dto.response.ResponseData;
import com.eefood.reactionservice.livestream.service.LiveStreamService;
import com.eefood.reactionservice.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/v1/livestreams")
@RequiredArgsConstructor
public class LiveStreamController {
  private final LiveStreamService liveStreamService;
  private final SecurityUtil securityUtil;

  @GetMapping("/check")
  public ResponseData<LiveStreamResponse> checkUserStream(@RequestParam Long userId) {
    var response = liveStreamService.checkUserStream(userId);
    return new ResponseData<>(HttpStatus.OK.value(), "success", response);
  }

  @PostMapping("/schedule")
  public ResponseData<LiveStreamResponse> scheduleLive(
    @RequestParam String description,
    @RequestParam String scheduledAt
  ) {
    Long userId = securityUtil.getCurrentUserId();
    LocalDateTime time = LocalDateTime.parse(scheduledAt);
    var res = liveStreamService.scheduleLive(userId, description, time);
    return new ResponseData<>(HttpStatus.OK.value(), "success", res);
  }

  @PostMapping("/start")
  public ResponseData<LiveStreamResponse> startLiveStream(
    @RequestParam(required = false) Long liveStreamId,
    @RequestParam(required = false) String description
  ) {
    Long userId = securityUtil.getCurrentUserId();
    return new ResponseData<>(HttpStatus.OK.value(), "success", liveStreamService.startLiveStream(userId, liveStreamId, description));
  }

  @PostMapping("/{liveStreamId}/end")
  public ResponseData<LiveStreamResponse> endLiveStream(@PathVariable Long liveStreamId) {
    Long userId = securityUtil.getCurrentUserId();
    return new ResponseData<>(HttpStatus.OK.value(), "success", liveStreamService.endLiveStream(liveStreamId, userId));
  }

  @GetMapping("/{liveStreamId}")
  public ResponseData<LiveStreamResponse> getLiveStream(@PathVariable Long liveStreamId) {
    Long userId = securityUtil.getCurrentUserId();
    return new ResponseData<>(HttpStatus.OK.value(), "success", liveStreamService.getLiveStream(liveStreamId, userId));
  }

//  @GetMapping("/{liveStreamId}/stats")
//  public ResponseData<LiveStreamResponse> getLiveStreamStats(@PathVariable Long liveStreamId) {
////    return
//  }
}