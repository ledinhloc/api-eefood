package com.eefood.reactionservice.livestream.controller;

import com.eefood.reactionservice.livestream.dto.ws.SubtitleSubscriptionRequest;
import com.eefood.reactionservice.livestream.service.LiveSubtitlePreferenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
@RequiredArgsConstructor
public class LiveSubtitleSocketController {

  private final LiveSubtitlePreferenceService subtitlePreferenceService;

  @MessageMapping("/live/subtitles/register")
  public void registerSubtitle(
    SubtitleSubscriptionRequest request,
    SimpMessageHeaderAccessor headerAccessor,
    Principal principal
  ) {
    subtitlePreferenceService.register(
      principal,
      headerAccessor.getSessionId(),
      request
    );
  }

  @MessageMapping("/live/subtitles/unregister")
  public void unregisterSubtitle(SimpMessageHeaderAccessor headerAccessor) {
    subtitlePreferenceService.unregister(headerAccessor.getSessionId());
  }
}
