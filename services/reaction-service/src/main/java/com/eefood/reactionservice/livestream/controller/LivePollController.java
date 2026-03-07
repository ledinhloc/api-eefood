package com.eefood.reactionservice.livestream.controller;

import com.eefood.reactionservice.dto.response.ResponseData;
import com.eefood.reactionservice.livestream.dto.request.CreateLivePollRequest;
import com.eefood.reactionservice.livestream.dto.response.LivePollResponse;
import com.eefood.reactionservice.livestream.dto.response.PollResultResponse;
import com.eefood.reactionservice.livestream.enums.PollStatus;
import com.eefood.reactionservice.livestream.service.LivePollService;
import com.eefood.reactionservice.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.common.requests.VoteRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/livestreams/{liveStreamId}/polls")
@RequiredArgsConstructor
public class LivePollController {
  private final LivePollService livePollService;
  private final SecurityUtil securityUtil;

  @GetMapping("/active")
  public ResponseData<LivePollResponse> getActivePoll(@PathVariable Long liveStreamId) {
    return new ResponseData<>(
      HttpStatus.OK.value(),
      "Success",
      livePollService.getActivePoll(liveStreamId)
    );
  }

  @PatchMapping("/{pollId}/status")
  public ResponseData<LivePollResponse> updateStatus(
    @PathVariable Long liveStreamId,
    @PathVariable Long pollId,
    @RequestParam PollStatus pollStatus
  ) {
    Long userId = securityUtil.getCurrentUserId();
    return new ResponseData<>(
      HttpStatus.OK.value(),
      "Success",
      livePollService.updateStatus(liveStreamId, pollId, userId, pollStatus)
    );
  }

  @PostMapping
  public ResponseData<LivePollResponse> create(@PathVariable Long liveStreamId,
                             @RequestBody CreateLivePollRequest req) {
    return new ResponseData<>(HttpStatus.OK.value(), "Success", livePollService.create(req, liveStreamId));
  }

  @GetMapping("/{pollId}")
  public ResponseData<LivePollResponse> detail(@PathVariable Long liveStreamId,
                                 @PathVariable Long pollId) {
    return new ResponseData<>(HttpStatus.OK.value(), "Success", livePollService.detail(liveStreamId, pollId));
  }

  @PostMapping("/{pollId}/vote")
  public ResponseData<PollResultResponse> vote(
                                  @PathVariable Long liveStreamId,
                                 @PathVariable Long pollId,
                                 @RequestParam List<Long> optionIds) {
    Long userId = securityUtil.getCurrentUserId();
    return new ResponseData<>(HttpStatus.OK.value(), "Success", livePollService.vote(liveStreamId, pollId, userId, optionIds));
  }

  @GetMapping("/{pollId}/result")
  public ResponseData<PollResultResponse> result(@PathVariable Long pollId) {
    return new ResponseData<>(HttpStatus.OK.value(), "Success", livePollService.buildResult(pollId));
  }
}
