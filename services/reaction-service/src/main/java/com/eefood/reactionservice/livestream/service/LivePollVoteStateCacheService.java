package com.eefood.reactionservice.livestream.service;

import com.eefood.reactionservice.livestream.dto.cache.PollUserVoteState;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LivePollVoteStateCacheService {
  public static final String POLL_USER_VOTE_STATE_CACHE = "poll-user-vote-state";

  private final CacheManager cacheManager;

  public Set<Long> getOptionIds(Long pollId, Long userId) {
    // Đọc state vote hiện tại của một user trong poll từ Redis.
    PollUserVoteState state = getCache().get(buildKey(pollId, userId), PollUserVoteState.class);
    if (state == null || state.getOptionIds() == null) {
      return Collections.emptySet();
    }
    return new HashSet<>(state.getOptionIds());
  }

  public void saveOptionIds(Long pollId, Long userId, Set<Long> optionIds) {
    if (optionIds == null || optionIds.isEmpty()) {
      evict(pollId, userId);
      return;
    }

    getCache().put(
      buildKey(pollId, userId),
      PollUserVoteState.builder()
        .pollId(pollId)
        .userId(userId)
        .optionIds(new HashSet<>(optionIds))
        .build()
    );
  }

  public void evict(Long pollId, Long userId) {
    // Xóa state khi user không còn vote nào trong poll.
    getCache().evict(buildKey(pollId, userId));
  }

  private String buildKey(Long pollId, Long userId) {
    return pollId + ":" + userId;
  }

  private Cache getCache() {
    Cache cache = cacheManager.getCache(POLL_USER_VOTE_STATE_CACHE);
    if (cache == null) {
      throw new IllegalStateException("Cache '" + POLL_USER_VOTE_STATE_CACHE + "' is not configured");
    }
    return cache;
  }
}
