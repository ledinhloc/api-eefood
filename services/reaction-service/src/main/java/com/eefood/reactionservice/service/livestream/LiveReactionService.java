package com.eefood.reactionservice.service.livestream;

import com.eefood.reactionservice.dto.response.LiveReactionResponse;
import com.eefood.reactionservice.dto.response.UserInfo;
import com.eefood.reactionservice.enums.ErrorMessage;
import com.eefood.reactionservice.enums.FoodEmotion;
import com.eefood.reactionservice.exception.ExceptionUtil;
import com.eefood.reactionservice.mapper.LiveReactionMapper;
import com.eefood.reactionservice.model.livestream.LiveReaction;
import com.eefood.reactionservice.model.livestream.LiveStream;
import com.eefood.reactionservice.repository.httpclient.IamClient;
import com.eefood.reactionservice.repository.livestream.LiveReactionRepository;
import com.eefood.reactionservice.repository.livestream.LiveStreamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LiveReactionService {
  private final LiveReactionRepository liveReactionRepository;
  private final LiveStreamRepository liveStreamRepository;
  private final IamClient iamClient;
  private final LiveReactionMapper mapper;
  private final SimpMessagingTemplate messagingTemplate;

  public LiveReactionResponse createReaction(Long userId, Long liveId, FoodEmotion emotion) {
    LiveStream liveStream = liveStreamRepository.findById(liveId)
      .orElseThrow(() -> ExceptionUtil.notFound(ErrorMessage.LIVE_STREAM_NOT_FOUND));

    LiveReaction reaction = LiveReaction.builder()
      .userId(userId)
      .emotion(emotion)
      .liveStream(liveStream)
      .build();

    liveReactionRepository.save(reaction);
    UserInfo user = iamClient.getUserInfo(userId).getData();
    LiveReactionResponse response = mapper.toResponse(reaction);
    response.setUsername(user.getUsername());
    response.setAvatarUrl(user.getAvatarUrl());

    // Broadcast đến tất cả viewer của live stream
    messagingTemplate.convertAndSend(
      "/topic/live-reaction/" + liveId,
      response
    );

    return response;
  }

  public List<LiveReactionResponse> getReactionsByStream(Long liveStreamId) {
    return liveReactionRepository.findByLiveStreamId(liveStreamId)
      .stream()
      .map(r ->{
        LiveReactionResponse res = mapper.toResponse(r);

        // lấy thông tin user
        UserInfo u = iamClient.getUserInfo(r.getUserId()).getData();
        res.setUsername(u.getUsername());
        res.setAvatarUrl(u.getAvatarUrl());

        return res;
      })
      .toList();
  }

}
