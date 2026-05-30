package com.eefood.reactionservice.livestream.service;

import com.eefood.reactionservice.dto.response.UserInfo;
import com.eefood.reactionservice.enums.ErrorMessage;
import com.eefood.reactionservice.enums.LiveStreamStatus;
import com.eefood.reactionservice.exception.ExceptionUtil;
import com.eefood.reactionservice.livestream.dto.request.SendGiftRequest;
import com.eefood.reactionservice.livestream.dto.response.LiveGiftItemResponse;
import com.eefood.reactionservice.livestream.dto.response.SendGiftResponse;
import com.eefood.reactionservice.livestream.mapper.LiveGiftMapper;
import com.eefood.reactionservice.livestream.model.LiveGiftItem;
import com.eefood.reactionservice.livestream.model.LiveStream;
import com.eefood.reactionservice.livestream.model.LivestreamGiftLog;
import com.eefood.reactionservice.livestream.repository.LiveStreamRepository;
import com.eefood.reactionservice.livestream.repository.LiveGiftItemRepository;
import com.eefood.reactionservice.livestream.repository.LivestreamGiftLogRepository;
import com.eefood.reactionservice.repository.httpclient.IamClient;
import com.eefood.reactionservice.service.payment.DiamondWalletService;
import com.eefood.reactionservice.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class LiveGiftService {
    private final SecurityUtil securityUtil;
    private final LiveGiftItemRepository liveGiftItemRepository;
    private final DiamondWalletService diamondWalletService;
    private final LiveStreamRepository liveStreamRepository;
    private final LivestreamGiftLogRepository livestreamGiftLogRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final LiveGiftMapper liveGiftMapper;
    private final IamClient iamClient;

    public List<LiveGiftItemResponse>  getAvailableGifts() {
        return liveGiftItemRepository.findByIsActiveTrue()
                .stream()
                .map(liveGiftMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public SendGiftResponse sendGift(SendGiftRequest request) {
        Long senderId = securityUtil.getCurrentUserId();

        LiveStream liveStream = liveStreamRepository
                .findByIdAndStatus(request.getLivestreamId(), LiveStreamStatus.LIVE)
                .orElseThrow(() -> ExceptionUtil.notFound(ErrorMessage.LIVE_STREAM_NOT_FOUND));

        Long receiverId = liveStream.getUserId();

        if (senderId.equals(receiverId)) {
            throw new IllegalArgumentException("Cannot send gift to yourself");
        }
        if (request.getQuantity() < 1 || request.getQuantity() > 99) {
            throw new IllegalArgumentException("Quantity must be between 1 and 99");
        }

        LiveGiftItem gift = liveGiftItemRepository
                .findByIdAndIsActiveTrue(request.getGiftItemId())
                .orElseThrow(() -> new IllegalArgumentException("Gift not found or inactive"));

        long totalCost = gift.getDiamondCost() * request.getQuantity();
        long hostReceives = totalCost * gift.getHostSharePercent() / 100;

        diamondWalletService.spend(senderId, totalCost);

        LivestreamGiftLog giftLog = LivestreamGiftLog.builder()
                .senderId(senderId)
                .receiverId(receiverId)
                .liveStream(liveStream)
                .liveGiftItem(gift)
                .quantity(request.getQuantity())
                .totalDiamondSpent(totalCost)
                .hostDiamondReceived(hostReceives)
                .build();
        giftLog = livestreamGiftLogRepository.save(giftLog);

        diamondWalletService.topup(receiverId, hostReceives, null);

        log.info("Gift sent: sender={}, receiver={}, gift={}, qty={}, cost={}, hostGets={}",
                senderId, receiverId, gift.getName(),
                request.getQuantity(), totalCost, hostReceives);

        UserInfo userInfo = iamClient.getUserInfo(senderId).getData();

        SendGiftResponse response = buildResponse(giftLog, gift, senderId, userInfo);

        try {
            messagingTemplate.convertAndSend(
                    "/topic/live-gift/" + request.getLivestreamId(),
                    response
            );
            log.info(" Broadcasted live gift id={} to live={}", request.getGiftItemId(), request.getLivestreamId());
        } catch (Exception e) {
            log.error(" Failed to broadcast gift", e);
        }

        return response;
    }

    private SendGiftResponse buildResponse(LivestreamGiftLog log, LiveGiftItem gift, Long senderId, UserInfo userInfo) {
        return SendGiftResponse.builder()
                .giftLogId(log.getId())
                .senderId(senderId)
                .senderName(userInfo.getUsername())
                .senderImageUrl(userInfo.getAvatarUrl())
                .receiverId(log.getReceiverId())
                .livestreamId(log.getLiveStream().getId())
                .giftItemId(gift.getId())
                .giftName(gift.getName())
                .animationUrl(gift.getAnimationUrl() != null ? gift.getAnimationUrl() : gift.getImageUrl())
                .quantity(log.getQuantity())
                .totalDiamondSpent(log.getTotalDiamondSpent())
                .senderNewBalance(diamondWalletService.getBalance(senderId))
                .build();
    }
}
