package com.eefood.reactionservice.livestream.service;

import com.eefood.reactionservice.dto.response.UserInfo;
import com.eefood.reactionservice.enums.ErrorMessage;
import com.eefood.reactionservice.exception.ExceptionUtil;
import com.eefood.reactionservice.livestream.dto.response.BlockUserResponse;
import com.eefood.reactionservice.livestream.mapper.LiveStreamBlockMapper;
import com.eefood.reactionservice.livestream.model.LiveStreamBlock;
import com.eefood.reactionservice.livestream.repository.LiveStreamBlockRepository;
import com.eefood.reactionservice.repository.httpclient.IamClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LiveStreamBlockService {
  private final LiveStreamBlockRepository repo;
  private final IamClient iamClient;
  private final LiveStreamBlockMapper mapper;

  public BlockUserResponse blockUser(Long streamerId, Long blockedUserId) {

    if (repo.existsByStreamerIdAndBlockedUserId(streamerId, blockedUserId)) {
      throw ExceptionUtil.conflict(ErrorMessage.USER_ALREADY_BLOCKED);
    }

    LiveStreamBlock block = LiveStreamBlock.builder()
      .streamerId(streamerId)
      .blockedUserId(blockedUserId)
      .createdAt(LocalDateTime.now())
      .build();

    repo.save(block);
    //info user
    var userRes = iamClient.getUserInfo(blockedUserId);
    if (userRes == null || userRes.getData() == null) {
      throw ExceptionUtil.notFound(ErrorMessage.USER_INFO_NOT_FOUND);
    }

    return mapper.toResponse(block, userRes.getData());
  }

  @Transactional
  public void unblockUser(Long streamerId, Long blockedUserId) {
    boolean exists = repo.existsByStreamerIdAndBlockedUserId(streamerId, blockedUserId);
    if (!exists) {
      throw ExceptionUtil.notFound(ErrorMessage.USER_NOT_BLOCKED);
    }

    repo.deleteByStreamerIdAndBlockedUserId(streamerId, blockedUserId);
  }

  public List<BlockUserResponse> getBlockedUsers(Long streamerId) {

    // Lấy danh sách block (sorted DESC)
    var blocks = repo.findAllByStreamerIdOrderByCreatedAtDesc(streamerId);
    if(blocks.isEmpty()) return List.of();

    // Lấy list userId của người bị block
    List<Long> userIds = blocks.stream()
      .map(LiveStreamBlock::getBlockedUserId)
      .toList();

    // Gọi batch API từ iam-service
    var userBatchRes = iamClient.getUserInfoBatch(userIds);
    if(userBatchRes == null || userBatchRes.getData() == null) {
      throw ExceptionUtil.notFound(ErrorMessage.USER_BATCH_INFO_FAILED);
    }

    // Map userInfo theo userId
    Map<Long, UserInfo> infoMap = userBatchRes.getData().stream()
      .collect(Collectors.toMap(UserInfo::getId, u -> u));

    // Merge block + userInfo
    return blocks.stream()
      .map(block -> mapper.toResponse(
        block,
        infoMap.get(block.getBlockedUserId())
      ))
      .toList();
  }

  public boolean isUserBlocked(Long streamerId, Long userId) {
    return repo.existsByStreamerIdAndBlockedUserId(streamerId, userId);
  }
}
