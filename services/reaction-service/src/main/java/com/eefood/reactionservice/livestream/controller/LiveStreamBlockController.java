package com.eefood.reactionservice.livestream.controller;

import com.eefood.reactionservice.dto.response.ResponseData;
import com.eefood.reactionservice.livestream.dto.response.BlockUserResponse;
import com.eefood.reactionservice.livestream.service.LiveStreamBlockService;
import com.eefood.reactionservice.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/livestreams/block")
@RequiredArgsConstructor
public class LiveStreamBlockController {
  private final LiveStreamBlockService blockService;
  private final SecurityUtil securityUtil;

  @PostMapping
  public ResponseData<BlockUserResponse> block(@RequestParam Long blockedUserId) {
    Long streamerId = securityUtil.getCurrentUserId();
    var res = blockService.blockUser(streamerId, blockedUserId);
    return new ResponseData<>(200, "blocked", res);
  }

  @DeleteMapping()
  public ResponseData<String> unblock(@RequestParam Long blockedUserId) {
    Long streamerId = securityUtil.getCurrentUserId();
    blockService.unblockUser(streamerId, blockedUserId);
    return new ResponseData<>(200, "unblocked", "OK");
  }

  @GetMapping
  public ResponseData<List<BlockUserResponse>> getBlockedList() {
    Long streamerId = securityUtil.getCurrentUserId();
    var list = blockService.getBlockedUsers(streamerId);
    return new ResponseData<>(200, "success", list);
  }
}
