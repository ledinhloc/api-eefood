package com.eefood.reactionservice.livestream.service;

import com.eefood.reactionservice.livestream.repository.LivePollOptionRepository;
import com.eefood.reactionservice.livestream.repository.LivePollVoteRepository;
import com.eefood.reactionservice.livestream.model.LivePollVote;
import jakarta.annotation.PostConstruct;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.RedisSystemException;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.connection.stream.StreamReadOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class LivePollVoteStreamConsumer {
  private static final String GROUP = "live-poll-db-writers";
  private static final String CONSUMER = "reaction-service";

  private final StringRedisTemplate stringRedisTemplate;
  private final LivePollService livePollService;

  @PostConstruct
  public void ensureGroup() {
    // Tạo consumer group một lần khi app khởi động để có thể ack/retry message.
    try {
      stringRedisTemplate.opsForStream().createGroup(
        LivePollVoteStreamProducer.VOTE_STREAM_KEY,
        ReadOffset.latest(),
        GROUP
      );
    } catch (Exception ex) {
      if (isStreamMissing(ex)) {
        stringRedisTemplate.opsForStream().add(
          MapRecord.create(
            LivePollVoteStreamProducer.VOTE_STREAM_KEY,
            Map.of("system", "bootstrap")
          )
        );
        try {
          stringRedisTemplate.opsForStream().createGroup(
            LivePollVoteStreamProducer.VOTE_STREAM_KEY,
            ReadOffset.latest(),
            GROUP
          );
        } catch (Exception ignored) {
          log.debug("Vote stream group already initialized");
        }
      } else {
        log.debug("Vote stream group already initialized");
      }
    }
  }

  @Scheduled(fixedDelay = 1000L)
  public void consumeVoteEvents() {
    // Poll Redis Stream theo nhịp ngắn để flush vote xuống DB gần realtime.
    List<MapRecord<String, Object, Object>> records = stringRedisTemplate.opsForStream().read(
      Consumer.from(GROUP, CONSUMER),
      StreamReadOptions.empty().count(100).block(Duration.ofSeconds(1)),
      StreamOffset.create(LivePollVoteStreamProducer.VOTE_STREAM_KEY, ReadOffset.lastConsumed())
    );

    if (records == null || records.isEmpty()) {
      return;
    }

    for (MapRecord<String, Object, Object> record : records) {
      try {
        if (isBootstrap(record.getValue())) {
          stringRedisTemplate.opsForStream().acknowledge(GROUP, record);
          continue;
        }

        Long pollId = Long.valueOf(String.valueOf(record.getValue().get("pollId")));
        Long userId = Long.valueOf(String.valueOf(record.getValue().get("userId")));
        List<Long> toRemove = parseIds(record.getValue().get("toRemove"));
        List<Long> toAdd = parseIds(record.getValue().get("toAdd"));

        livePollService.persistVoteEvent(pollId, userId, toAdd, toRemove);
        stringRedisTemplate.opsForStream().acknowledge(GROUP, record);
      } catch (Exception ex) {
        log.error("Failed to persist vote event from stream id={}", record.getId(), ex);
      }
    }
  }

  private boolean isBootstrap(Map<Object, Object> body) {
    return body.containsKey("system");
  }

  private boolean isStreamMissing(Exception ex) {
    return ex instanceof RedisSystemException
      || (ex.getMessage() != null && ex.getMessage().contains("no such key"));
  }

  private List<Long> parseIds(Object rawValue) {
    // Chuyển payload CSV từ stream về list id để xử lý DB.
    if (rawValue == null) {
      return Collections.emptyList();
    }

    String value = String.valueOf(rawValue).trim();
    if (value.isEmpty()) {
      return Collections.emptyList();
    }

    return java.util.Arrays.stream(value.split(","))
      .filter(part -> !part.isBlank())
      .map(String::trim)
      .map(Long::valueOf)
      .toList();
  }
}
