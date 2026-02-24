package com.eefood.reactionservice.livestream.controller;

import com.eefood.reactionservice.dto.response.ResponseData;
import com.eefood.reactionservice.livestream.dto.response.ViewerResponse;
import com.eefood.reactionservice.livestream.service.LiveViewerService;
import com.eefood.reactionservice.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/livestreams/{liveStreamId}/viewers")
@RequiredArgsConstructor
public class LiveViewerController {
  private final LiveViewerService liveViewerService;

  private final SecurityUtil securityUtil;

  @PostMapping("/join")
  public ResponseData<String> join(@PathVariable Long liveStreamId) {
    Long userId = securityUtil.getCurrentUserId();
    liveViewerService.joinLive(liveStreamId, userId);
    return new ResponseData<>(200, "Joined successfully", null);
  }

  @PostMapping("/leave")
  public ResponseData<String> leave(@PathVariable Long liveStreamId) {
    Long userId = securityUtil.getCurrentUserId();
    liveViewerService.leaveLive(liveStreamId, userId);
    return new ResponseData<>(200, "Left successfully", null);
  }

  @GetMapping
  public ResponseData<List<ViewerResponse>> getViewers(@PathVariable Long liveStreamId) {
    List<ViewerResponse> viewers = liveViewerService.getCurrentViewers(liveStreamId);
    return new ResponseData<>(200, "Success", viewers);
  }
}
