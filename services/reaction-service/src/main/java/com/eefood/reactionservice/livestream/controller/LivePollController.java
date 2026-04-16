package com.eefood.reactionservice.livestream.controller;

import com.eefood.reactionservice.dto.response.ResponseData;
import com.eefood.reactionservice.livestream.dto.request.CreateLivePollRequest;
import com.eefood.reactionservice.livestream.dto.response.LivePollResponse;
import com.eefood.reactionservice.livestream.dto.response.LivePollOptionProposalResponse;
import com.eefood.reactionservice.livestream.dto.response.PollOptionVotersResponse;
import com.eefood.reactionservice.livestream.dto.response.PollResultResponse;
import com.eefood.reactionservice.livestream.enums.PollOptionProposalStatus;
import com.eefood.reactionservice.livestream.enums.PollStatus;
import com.eefood.reactionservice.livestream.service.LivePollOptionProposalService;
import com.eefood.reactionservice.livestream.service.LivePollResultCacheService;
import com.eefood.reactionservice.livestream.service.LivePollService;
import com.eefood.reactionservice.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/livestreams/{liveStreamId}/polls")
@RequiredArgsConstructor
public class LivePollController {
  private final LivePollService livePollService;
  private final LivePollResultCacheService livePollResultCacheService;
  private final LivePollOptionProposalService livePollOptionProposalService;
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

  @PostMapping("/{pollId}/testVote")
  public ResponseData<PollResultResponse> testVote(
                                  @PathVariable Long liveStreamId,
                                 @PathVariable Long pollId,
                                 @RequestParam List<Long> optionIds,
                                 @RequestParam Long userId
                                ) {
    return new ResponseData<>(HttpStatus.OK.value(), "Success", livePollService.vote(liveStreamId, pollId, userId, optionIds));
  }

  @GetMapping("/{pollId}/result")
  public ResponseData<PollResultResponse> result(@PathVariable Long pollId) {
    return new ResponseData<>(HttpStatus.OK.value(), "Success", livePollResultCacheService.getResult(pollId));
  }

  @PostMapping("/{pollId}/option-proposals")
  public ResponseData<LivePollOptionProposalResponse> createOptionProposal(
    @PathVariable Long liveStreamId,
    @PathVariable Long pollId,
    @RequestParam String req
  ) {
    Long userId = securityUtil.getCurrentUserId();
    return new ResponseData<>(
      HttpStatus.OK.value(),
      "Success",
      livePollOptionProposalService.createProposal(liveStreamId, pollId, userId, req)
    );
  }

  @GetMapping("/{pollId}/option-proposals")
  public ResponseData<List<LivePollOptionProposalResponse>> getOptionProposals(
    @PathVariable Long liveStreamId,
    @PathVariable Long pollId,
    @RequestParam(required = false) PollOptionProposalStatus status
  ) {
    Long userId = securityUtil.getCurrentUserId();
    return new ResponseData<>(
      HttpStatus.OK.value(),
      "Success",
      livePollOptionProposalService.getProposals(liveStreamId, pollId, userId, status)
    );
  }

  @PatchMapping("/{pollId}/option-proposals/{proposalId}/status")
  public ResponseData<LivePollOptionProposalResponse> updateOptionProposalStatus(
    @PathVariable Long liveStreamId,
    @PathVariable Long pollId,
    @PathVariable Long proposalId,
    @RequestParam PollOptionProposalStatus status
  ) {
    Long userId = securityUtil.getCurrentUserId();
    return new ResponseData<>(
      HttpStatus.OK.value(),
      "Success",
      livePollOptionProposalService.updateProposalStatus(
        liveStreamId,
        pollId,
        proposalId,
        userId,
        status
      )
    );
  }

  @GetMapping("/{pollId}/options/{optionId}/voters")
  public ResponseData<PollOptionVotersResponse> getOptionVoters(
    @PathVariable Long liveStreamId,
    @PathVariable Long pollId,
    @PathVariable Long optionId
  ) {
    return new ResponseData<>(
      HttpStatus.OK.value(),
      "Success",
      livePollService.getOptionVoters(liveStreamId, pollId, optionId)
    );
  }
}
