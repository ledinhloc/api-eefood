package com.eefood.reactionservice.livestream.service;

import com.eefood.common.avro.LivePollVoteEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LivePollVoteKafkaConsumer {

  private final LivePollService livePollService;

  @KafkaListener(
    topics = LivePollVoteKafkaProducer.VOTE_TOPIC,
    groupId = "live-poll-db-writers",
    containerFactory = "livePollKafkaListenerContainerFactory"
  )
  public void consume(LivePollVoteEvent event) {
    livePollService.persistVoteEvent(
      event.getPollId(),
      event.getUserId(),
      event.getToAdd(),
      event.getToRemove()
    );
  }
}
