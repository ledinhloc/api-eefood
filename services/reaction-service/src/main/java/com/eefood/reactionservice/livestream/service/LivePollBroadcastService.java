package com.eefood.reactionservice.livestream.service;

import com.eefood.reactionservice.livestream.dto.response.LivePollResponse;
import com.eefood.reactionservice.livestream.dto.response.PollResultResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class LivePollBroadcastService {

  private final SimpMessagingTemplate messagingTemplate;

  public void broadcastPoll(Long liveStreamId, LivePollResponse response) {
    try {
      messagingTemplate.convertAndSend(
        "/topic/live-poll/" + liveStreamId,
        response
      );
      log.info("Broadcasted poll update for livestream {}", liveStreamId);
    } catch (Exception e) {
      log.error("Error broadcasting poll update for livestream {}", liveStreamId, e);
    }
  }

  public void broadcastPollResult(Long liveStreamId, PollResultResponse response) {
    try {
      messagingTemplate.convertAndSend(
        "/topic/live-poll-result/" + liveStreamId,
        response
      );
      log.info("Broadcasted poll result for livestream {}", liveStreamId);
    } catch (Exception e) {
      log.error("Error broadcasting poll result for livestream {}", liveStreamId, e);
    }
  }
}