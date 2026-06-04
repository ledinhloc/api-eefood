package com.eefood.reactionservice.livestream.service;

import com.eefood.reactionservice.dto.response.UserInfo;
import com.eefood.reactionservice.livestream.dto.response.LeaderboardEntryResponse;
import com.eefood.reactionservice.repository.httpclient.IamClient;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class LiveLeaderboardService {
    private static final String LEADERBOARD_KEY = "live:leaderboard:";
    private static final String USER_INFO_KEY   = "live:leaderboard:info:";
    private static final Duration TTL = Duration.ofHours(6);
    private static final int TOP_N = 10;

    private final StringRedisTemplate redisTemplate;
    private final SimpMessagingTemplate messagingTemplate;
    private final IamClient iamClient;

   // Cộng kim cương cho viewer
    public void recordAndBroadcast(Long livestreamId, Long userId, long diamonds) {
        String boardKey = LEADERBOARD_KEY + livestreamId;

        redisTemplate.opsForZSet().incrementScore(boardKey, userId.toString(), diamonds);
        redisTemplate.expire(boardKey, TTL);

        cacheUserInfoIfAbsent(livestreamId, userId);

        List<LeaderboardEntryResponse> top10 = getTop10(livestreamId);
        broadcastLeaderboard(livestreamId, top10);
    }

    //  Lấy top 10 hiện tại
    public List<LeaderboardEntryResponse> getTop10(Long livestreamId) {
        String boardKey = LEADERBOARD_KEY + livestreamId;

        Set<ZSetOperations.TypedTuple<String>> entries =
                redisTemplate.opsForZSet().reverseRangeWithScores(boardKey, 0, TOP_N - 1);

        if (entries == null || entries.isEmpty()) return List.of();

        List<LeaderboardEntryResponse> result = new ArrayList<>();
        int rank = 1;
        for (ZSetOperations.TypedTuple<String> entry : entries) {
            Long userId = Long.parseLong(entry.getValue());
            long score  = entry.getScore().longValue();
            Map<Object, Object> info = getUserInfo(livestreamId, userId);

            result.add(LeaderboardEntryResponse.builder()
                    .rank(rank++)
                    .userId(userId)
                    .username((String) info.getOrDefault("username", "Unknown"))
                    .avatarUrl((String) info.getOrDefault("avatarUrl", ""))
                    .totalDiamonds(score)
                    .build());
        }
        return result;
    }

    private void cacheUserInfoIfAbsent(Long livestreamId, Long userId) {
        String infoKey = USER_INFO_KEY + livestreamId + ":" + userId;
        if (Boolean.TRUE.equals(redisTemplate.hasKey(infoKey))) return;

        try {
            UserInfo info = iamClient.getUserInfo(userId).getData();
            redisTemplate.opsForHash().put(infoKey, "username", info.getUsername());
            redisTemplate.opsForHash().put(infoKey, "avatarUrl",
                    info.getAvatarUrl() != null ? info.getAvatarUrl() : "");
            redisTemplate.expire(infoKey, TTL);
        } catch (Exception e) {
            log.warn("Could not cache user info for userId={}", userId, e);
        }
    }

    private Map<Object, Object> getUserInfo(Long livestreamId, Long userId) {
        String infoKey = USER_INFO_KEY + livestreamId + ":" + userId;
        Map<Object, Object> info = redisTemplate.opsForHash().entries(infoKey);
        if (info == null || info.isEmpty()) {
            try {
                UserInfo u = iamClient.getUserInfo(userId).getData();
                return Map.of("username", u.getUsername(),
                        "avatarUrl", u.getAvatarUrl() != null ? u.getAvatarUrl() : "");
            } catch (Exception e) {
                return Map.of("username", "Unknown", "avatarUrl", "");
            }
        }
        return info;
    }

    private void broadcastLeaderboard(Long livestreamId, List<LeaderboardEntryResponse> top10) {
        try {
            messagingTemplate.convertAndSend(
                    "/topic/live-leaderboard/" + livestreamId, top10);
        } catch (Exception e) {
            log.error("Failed to broadcast leaderboard for liveId={}", livestreamId, e);
        }
    }
}
