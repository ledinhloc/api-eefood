package com.eefood.reactionservice.livestream.controller;

import com.eefood.reactionservice.dto.response.ResponseData;
import com.eefood.reactionservice.livestream.dto.request.CreateLivePollRequest;
import com.eefood.reactionservice.livestream.dto.response.LivePollResponse;
import com.eefood.reactionservice.livestream.dto.response.PollResultResponse;
import com.eefood.reactionservice.livestream.service.LivePollService;
import com.eefood.reactionservice.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.common.requests.VoteRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/livestreams/{liveStreamId}/polls")
@RequiredArgsConstructor
public class LivePollController {
  private final LivePollService livePollService;
  private final SecurityUtil securityUtil;

  @PostMapping
  public ResponseData<LivePollResponse> create(@PathVariable Long liveStreamId,
                             @RequestBody CreateLivePollRequest req) {
    req.setLiveStreamId(liveStreamId);
    return new ResponseData<>(HttpStatus.OK.value(), "Success", livePollService.create(req));
  }

  @GetMapping("/{pollId}")
  public ResponseData<LivePollResponse> detail(@PathVariable Long liveStreamId,
                                 @PathVariable Long pollId) {
    return new ResponseData<>(HttpStatus.OK.value(), "Success", livePollService.detail(liveStreamId, pollId));
  }

  @PostMapping("/{pollId}/vote")
  public ResponseData<PollResultResponse> vote(@PathVariable Long liveStreamId,
                                 @PathVariable Long pollId,
                                 @RequestParam Long optionId) {
    Long userId = securityUtil.getCurrentUserId();
    return new ResponseData<>(HttpStatus.OK.value(), "Success", livePollService.vote(liveStreamId, pollId, userId, optionId));
  }

  @GetMapping("/{pollId}/result")
  public ResponseData<PollResultResponse> result(@PathVariable Long pollId) {
    return new ResponseData<>(HttpStatus.OK.value(), "Success", livePollService.buildResult(pollId));
  }
}
