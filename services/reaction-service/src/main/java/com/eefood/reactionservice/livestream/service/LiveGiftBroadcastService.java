package com.eefood.reactionservice.livestream.service;

import com.eefood.reactionservice.livestream.dto.response.SendGiftResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class LiveGiftBroadcastService {
    private final SimpMessagingTemplate messagingTemplate;
    private final LiveLeaderboardService liveLeaderboardService;

    @Async("giftTaskExecutor")
    public void broadcastGiftAnimation(Long livestreamId, SendGiftResponse response) {
        try {
            messagingTemplate.convertAndSend(
                    "/topic/live-gift/" + livestreamId, response);
            log.info("Broadcasted gift id={} to live={}", response.getGiftItemId(), livestreamId);
        } catch (Exception e) {
            log.error("Failed to broadcast gift animation liveId={}", livestreamId, e);
        }
    }

    @Async("giftTaskExecutor")
    public void updateLeaderboard(Long livestreamId, Long senderId, long totalCost) {
        try {
            liveLeaderboardService.recordAndBroadcast(livestreamId, senderId, totalCost);
        } catch (Exception e) {
            log.error("Failed to update leaderboard liveId={}", livestreamId, e);
        }
    }
}
