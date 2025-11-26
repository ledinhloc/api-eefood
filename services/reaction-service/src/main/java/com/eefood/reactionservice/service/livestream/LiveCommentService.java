package com.eefood.reactionservice.service.livestream;

import com.eefood.reactionservice.dto.response.LiveCommentResponse;
import com.eefood.reactionservice.dto.response.UserInfo;
import com.eefood.reactionservice.model.livestream.LiveComment;
import com.eefood.reactionservice.model.livestream.LiveStream;
import com.eefood.reactionservice.repository.httpclient.IamClient;
import com.eefood.reactionservice.repository.livestream.LiveCommentRepository;
import com.eefood.reactionservice.repository.livestream.LiveStreamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LiveCommentService {
  private final LiveStreamRepository liveStreamRepository;
  private final LiveCommentRepository commentRepository;
  private final SimpMessagingTemplate messagingTemplate;
  private final IamClient iamClient;

  public List<LiveCommentResponse> getComments(Long liveStreamId) {

    List<LiveComment> comments = commentRepository
      .findAllByLiveStreamIdAndIsDeletedFalseOrderByCreatedAtAsc(liveStreamId);

    return comments.stream().map(c -> {
      UserInfo user = iamClient.getUserInfo(c.getUserId()).getData();

      return LiveCommentResponse.builder()
        .id(c.getId())
        .userId(user.getId())
        .username(user.getUsername())
        .avatarUrl(user.getAvatarUrl())
        .message(c.getMessage())
        .createdAt(c.getCreatedAt().toString())
        .build();
    }).toList();
  }

  public LiveCommentResponse addComment(Long liveStreamId, Long userId, String message) {

    LiveStream liveStream = liveStreamRepository.findById(liveStreamId)
      .orElseThrow(() -> new RuntimeException("LiveStream not found"));

    LiveComment comment = LiveComment.builder()
      .liveStream(liveStream)
      .userId(userId)
      .message(message)
      .isDeleted(false)
      .build();

    comment = commentRepository.save(comment);

    // Lấy thông tin user từ IAM
    UserInfo user = iamClient.getUserInfo(userId).getData();

    LiveCommentResponse response = LiveCommentResponse.builder()
      .id(comment.getId())
      .userId(user.getId())
      .username(user.getUsername())
      .avatarUrl(user.getAvatarUrl())
      .message(comment.getMessage())
      .createdAt(comment.getCreatedAt().toString())
      .build();

    // Broadcast ra WebSocket cho viewer
    messagingTemplate.convertAndSend(
      "/topic/live/" + liveStreamId + "/comments",
      response
    );

    return response;
  }

  public LiveCommentResponse updateComment(Long commentId, String newMessage) {
    LiveComment comment = commentRepository.findById(commentId)
      .orElseThrow(() -> new RuntimeException("Comment not found"));

    comment.setMessage(newMessage);
    comment = commentRepository.save(comment);

    UserInfo user = iamClient.getUserInfo(comment.getUserId()).getData();

    return LiveCommentResponse.builder()
      .id(comment.getId())
      .userId(user.getId())
      .username(user.getUsername())
      .avatarUrl(user.getAvatarUrl())
      .message(comment.getMessage())
      .createdAt(comment.getCreatedAt().toString())
      .build();
  }

  public void deleteComment(Long commentId) {
    LiveComment comment = commentRepository.findByIdAndIsDeletedFalse(commentId)
      .orElseThrow(() -> new RuntimeException("Comment not found"));
    comment.setIsDeleted(true);
    commentRepository.save(comment);
  }
}
