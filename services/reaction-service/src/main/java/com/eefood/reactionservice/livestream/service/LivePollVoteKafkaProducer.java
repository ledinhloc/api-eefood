package com.eefood.reactionservice.livestream.service;

import com.eefood.common.avro.LivePollVoteEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LivePollVoteKafkaProducer {
  public static final String VOTE_TOPIC = "live-poll-votes";

  private final KafkaTemplate<String, Object> kafkaTemplate;

  public void publishVoteEvent(LivePollVoteEvent event) {
    String key = event.getPollId() + ":" + event.getUserId();
    kafkaTemplate.send(VOTE_TOPIC, key, event);
  }
}
