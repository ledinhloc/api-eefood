package com.eefood.reactionservice.livestream.service;

import com.eefood.reactionservice.livestream.dto.response.LivePollOptionResponse;
import com.eefood.reactionservice.livestream.dto.response.PollResultResponse;
import com.eefood.reactionservice.livestream.mapper.LivePollMapper;
import com.eefood.reactionservice.livestream.model.LivePollOption;
import com.eefood.reactionservice.livestream.repository.LivePollOptionRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LivePollResultCacheService {
  public static final String POLL_RESULT_CACHE = "poll-results";

  private final CacheManager cacheManager;
  private final LivePollOptionRepository optionRepo;
  private final LivePollMapper pollMapper;

  public PollResultResponse getResult(Long pollId) {
    // Đọc snapshot kết quả từ cache trước, thiếu thì dựng lại từ DB.
    Cache cache = getCache();
    PollResultResponse cached = cache.get(pollId, PollResultResponse.class);
    if (cached != null) {
      return cached;
    }

    PollResultResponse loaded = loadFromDb(pollId);
    cache.put(pollId, loaded);
    return loaded;
  }

  public PollResultResponse applyVoteDelta(Long pollId, Map<Long, Long> optionDeltas) {
    // Áp dụng phần thay đổi nhỏ vào snapshot hiện tại thay vì build lại toàn bộ result.
    Cache cache = getCache();
    PollResultResponse snapshot = cache.get(pollId, PollResultResponse.class);
    if (snapshot == null) {
      snapshot = loadFromDb(pollId);
    } else {
      snapshot = copy(snapshot);
    }

    if (optionDeltas != null && !optionDeltas.isEmpty()) {
      for (LivePollOptionResponse option : snapshot.getOptions()) {
        Long delta = optionDeltas.get(option.getId());
        if (delta != null && delta != 0L) {
          long currentCount = option.getCount() == null ? 0L : option.getCount();
          option.setCount(currentCount + delta);
        }
      }
    }
    cache.put(pollId, snapshot);
    return snapshot;
  }

  public void evictResult(Long pollId) {
    getCache().evict(pollId);
  }

  private PollResultResponse loadFromDb(Long pollId) {
    List<LivePollOption> options = optionRepo.findByPollIdOrderByIdAsc(pollId);

    return PollResultResponse.builder()
      .pollId(pollId)
      .options(pollMapper.toOptionResponses(options))
      .build();
  }

  private PollResultResponse copy(PollResultResponse snapshot) {
    List<LivePollOptionResponse> copiedOptions = new ArrayList<>();
    if (snapshot.getOptions() != null) {
      for (LivePollOptionResponse option : snapshot.getOptions()) {
        copiedOptions.add(LivePollOptionResponse.builder()
          .id(option.getId())
          .text(option.getText())
          .count(option.getCount())
          .build());
      }
    }

    return PollResultResponse.builder()
      .pollId(snapshot.getPollId())
      .options(copiedOptions)
      .build();
  }

  private Cache getCache() {
    Cache cache = cacheManager.getCache(POLL_RESULT_CACHE);
    if (cache == null) {
      throw new IllegalStateException("Cache '" + POLL_RESULT_CACHE + "' is not configured");
    }
    return cache;
  }
}
