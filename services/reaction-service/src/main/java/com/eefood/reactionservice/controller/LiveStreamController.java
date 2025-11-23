package com.eefood.reactionservice.controller;

import com.eefood.reactionservice.dto.response.LiveStreamResponse;
import com.eefood.reactionservice.dto.response.ResponseData;
import com.eefood.reactionservice.model.livestream.LiveStream;
import com.eefood.reactionservice.service.LiveStreamService;
import com.eefood.reactionservice.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/livestreams")
@RequiredArgsConstructor
public class LiveStreamController {
  private final LiveStreamService liveStreamService;
  private final SecurityUtil securityUtil;

  @PostMapping("/start")
  public ResponseData<LiveStreamResponse> startLiveStream(@RequestParam String description) {
    Long userId = securityUtil.getCurrentUserId();
    return new ResponseData<>(HttpStatus.OK.value(), "success", liveStreamService.startLiveStream(userId, description));
  }

  @PostMapping("/{liveStreamId}/end")
  public ResponseData<LiveStreamResponse> endLiveStream(@PathVariable Long liveStreamId) {
    Long userId = securityUtil.getCurrentUserId();
    return new ResponseData<>(HttpStatus.OK.value(), "success", liveStreamService.endLiveStream(userId, liveStreamId));
  }

  @GetMapping("/{liveStreamId}")
  public ResponseData<LiveStreamResponse> getLiveStream(@PathVariable Long liveStreamId) {
    return new ResponseData<>(HttpStatus.OK.value(), "success", liveStreamService.getLiveStream(liveStreamId));
  }

//  @GetMapping("/{liveStreamId}/stats")
//  public ResponseData<LiveStreamResponse> getLiveStreamStats(@PathVariable Long liveStreamId) {
////    return
//  }
}