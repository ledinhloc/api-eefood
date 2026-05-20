package com.eefood.reactionservice.livestream.controller;

import com.eefood.reactionservice.livestream.dto.response.LiveAudioTranscriptionResponse;
import com.eefood.reactionservice.livestream.dto.request.LiveSubtitleTranscriptRequest;
import com.eefood.reactionservice.livestream.dto.response.LiveStreamResponse;
import com.eefood.reactionservice.dto.response.ResponseData;
import com.eefood.reactionservice.livestream.service.LiveStreamService;
import com.eefood.reactionservice.livestream.service.LiveSubtitleService;
import com.eefood.reactionservice.livestream.dto.ws.LiveSubtitleMessage;
import com.eefood.reactionservice.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/v1/livestreams")
@RequiredArgsConstructor
public class LiveStreamController {
  private final LiveStreamService liveStreamService;
  private final SecurityUtil securityUtil;
  private final LiveSubtitleService liveSubtitleService;

  @PostMapping(
    value = "/{liveStreamId}/subtitles/transcribe",
    consumes = MediaType.MULTIPART_FORM_DATA_VALUE
  )
  public ResponseData<LiveAudioTranscriptionResponse> transcribeAudio(
    @PathVariable Long liveStreamId,
    @RequestParam("audio") MultipartFile audio,
    @RequestParam(value = "language", required = false) String language
  ) {
    return new ResponseData<>(
      HttpStatus.OK.value(),
      "transcribe success",
      liveSubtitleService.transcribe(liveStreamId, audio, language)
    );
  }

  @PostMapping("/subtitles/transcripts")
  public ResponseData<LiveSubtitleMessage> publishSubtitleTranscript(
    @RequestBody LiveSubtitleTranscriptRequest request
  ) {
    return new ResponseData<>(
      HttpStatus.OK.value(),
      "subtitle broadcasted",
      liveSubtitleService.publishTranscript(request)
    );
  }

  @GetMapping("/check")
  public ResponseData<LiveStreamResponse> checkUserStream(@RequestParam Long userId) {
    var response = liveStreamService.checkUserStream(securityUtil.getCurrentUserId(), userId);
    return new ResponseData<>(HttpStatus.OK.value(), "success", response);
  }

  @PostMapping("/schedule")
  public ResponseData<LiveStreamResponse> scheduleLive(
    @RequestParam String description,
    @RequestParam String scheduledAt,
    @RequestParam(required = false) String spokenLanguage
  ) {
    Long userId = securityUtil.getCurrentUserId();
    LocalDateTime time = LocalDateTime.parse(scheduledAt);
    var res = liveStreamService.scheduleLive(userId, description, time, spokenLanguage);
    return new ResponseData<>(HttpStatus.OK.value(), "success", res);
  }

  @PostMapping("/start")
  public ResponseData<LiveStreamResponse> startLiveStream(
    @RequestParam(required = false) Long liveStreamId,
    @RequestParam(required = false) String description,
    @RequestParam(required = false) String spokenLanguage
  ) {
    Long userId = securityUtil.getCurrentUserId();
    return new ResponseData<>(HttpStatus.OK.value(), "success", liveStreamService.startLiveStream(userId, liveStreamId, description, spokenLanguage));
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
}
