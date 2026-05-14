package com.eefood.reactionservice.livestream.controller;


import com.eefood.reactionservice.livestream.dto.response.LiveCommentResponse;
import com.eefood.reactionservice.livestream.dto.response.LiveAudioTranscriptionResponse;
import com.eefood.reactionservice.dto.response.ResponseData;
import com.eefood.reactionservice.livestream.service.LiveCommentService;
import com.eefood.reactionservice.livestream.service.LiveSubtitleService;
import com.eefood.reactionservice.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/livestreams")
@RequiredArgsConstructor
public class LiveCommentController {

  private final LiveCommentService commentService;
  private final LiveSubtitleService liveSubtitleService;
  private final SecurityUtil securityUtil;

  @GetMapping("/{liveStreamId}/comments")
  public ResponseData<List<LiveCommentResponse>> getComments(@PathVariable Long liveStreamId) {
    return new ResponseData<>(HttpStatus.OK.value(), "success",commentService.getComments(liveStreamId));
  }

  @PostMapping("/{liveId}/comments")
  public ResponseData<LiveCommentResponse> createComment(@PathVariable Long liveId,
                                                         @RequestParam String message) {
    Long userId = securityUtil.getCurrentUserId();
    return new ResponseData<>(HttpStatus.OK.value(), "add success",
      commentService.addComment(liveId, userId, message)
    );
  }

  @PutMapping("/comments/{commentId}")
  public ResponseData<LiveCommentResponse> updateComment(
    @PathVariable Long commentId,
    @RequestParam String message
  ) {
    return new ResponseData<>(HttpStatus.OK.value(), "update success",
      commentService.updateComment(commentId, message)
    );
  }

  @DeleteMapping("/comments/{commentId}")
  public ResponseData<Void> deleteComment(@PathVariable Long commentId) {
    commentService.deleteComment(commentId);
    return new ResponseData<>(HttpStatus.OK.value(), "delete success");
  }

  @PostMapping(
    value = "/{liveStreamId}/subtitles/transcribe",
    consumes = MediaType.MULTIPART_FORM_DATA_VALUE
  )
  public ResponseData<LiveAudioTranscriptionResponse> transcribeAudio(
    @PathVariable Long liveStreamId,
    @RequestParam("audio") MultipartFile audio,
    @RequestParam(value = "language", defaultValue = "vi") String language
  ) {
    return new ResponseData<>(
      HttpStatus.OK.value(),
      "transcribe success",
      liveSubtitleService.transcribe(liveStreamId, audio, language)
    );
  }
}
