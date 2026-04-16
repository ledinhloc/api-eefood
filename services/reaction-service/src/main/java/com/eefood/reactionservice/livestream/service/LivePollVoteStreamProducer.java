package com.eefood.reactionservice.livestream.service;

import com.eefood.reactionservice.livestream.dto.event.LivePollVoteStreamEvent;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LivePollVoteStreamProducer {
  public static final String VOTE_STREAM_KEY = "stream:live-poll-votes";

  private final StringRedisTemplate stringRedisTemplate;

  public void publishVoteEvent(LivePollVoteStreamEvent event) {
    // Chuẩn hóa event thành payload string đơn giản để đẩy vào Redis Stream.
    Map<String, String> body = new HashMap<>();
    body.put("eventId", event.getEventId());
    body.put("liveStreamId", String.valueOf(event.getLiveStreamId()));
    body.put("pollId", String.valueOf(event.getPollId()));
    body.put("userId", String.valueOf(event.getUserId()));
    body.put("toAdd", joinIds(event.getToAdd()));
    body.put("toRemove", joinIds(event.getToRemove()));
    body.put("occurredAt", event.getOccurredAt() == null ? LocalDateTime.now().toString() : event.getOccurredAt().toString());

    // Ghi event để consumer nền xử lý persistence xuống DB.
    stringRedisTemplate.opsForStream().add(MapRecord.create(VOTE_STREAM_KEY, body));
  }

  private String joinIds(java.util.List<Long> ids) {
    // Redis Stream lưu field dạng string nên danh sách id được nén thành CSV.
    if (ids == null || ids.isEmpty()) {
      return "";
    }
    return ids.stream().map(String::valueOf).collect(java.util.stream.Collectors.joining(","));
  }
}
