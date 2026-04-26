package com.eefood.iamservice.service;

import com.eefood.iamservice.dto.request.UserHeightRequest;
import com.eefood.iamservice.dto.response.UserHeightResponse;
import com.eefood.iamservice.enums.ErrorMessage;
import com.eefood.iamservice.mapper.UserBodyMapper;
import com.eefood.iamservice.model.User;
import com.eefood.iamservice.model.UserHeight;
import com.eefood.iamservice.repository.UserHeightRepository;
import com.eefood.iamservice.repository.UserRepository;
import com.eefood.iamservice.utils.ExceptionUtil;
import com.eefood.iamservice.utils.SecurityUtil;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserHeightService {
  private final UserHeightRepository userHeightRepository;
  private final UserRepository userRepository;
  private final SecurityUtil securityUtil;
  private final UserBodyMapper userBodyMapper;

  public List<UserHeightResponse> getMyHeights() {
    Long userId = securityUtil.getCurrentUserId();
    return userHeightRepository.findAllByUser_IdOrderByRecordedAtDesc(userId).stream()
        .map(userBodyMapper::toHeightResponse)
        .toList();
  }

  @Transactional
  public UserHeightResponse createMyHeight(UserHeightRequest request) {
    User user = getCurrentUser();
    UserHeight userHeight =
        UserHeight.builder()
            .user(user)
            .heightCm(request.getHeightCm())
            .recordedAt(resolveRecordedAt(request.getRecordedAt()))
            .build();

    return userBodyMapper.toHeightResponse(userHeightRepository.save(userHeight));
  }

  @Transactional
  public UserHeightResponse updateMyHeight(Long heightId, UserHeightRequest request) {
    Long userId = securityUtil.getCurrentUserId();
    UserHeight userHeight =
        userHeightRepository
            .findByIdAndUser_Id(heightId, userId)
            .orElseThrow(() -> ExceptionUtil.notFound(ErrorMessage.USER_HEIGHT_NOT_FOUND));

    userHeight.setHeightCm(request.getHeightCm());
    if (request.getRecordedAt() != null) {
      userHeight.setRecordedAt(request.getRecordedAt());
    }

    return userBodyMapper.toHeightResponse(userHeightRepository.save(userHeight));
  }

  @Transactional
  public void deleteMyHeight(Long heightId) {
    Long userId = securityUtil.getCurrentUserId();
    UserHeight userHeight =
        userHeightRepository
            .findByIdAndUser_Id(heightId, userId)
            .orElseThrow(() -> ExceptionUtil.notFound(ErrorMessage.USER_HEIGHT_NOT_FOUND));

    userHeightRepository.delete(userHeight);
  }

  private User getCurrentUser() {
    Long userId = securityUtil.getCurrentUserId();
    return userRepository
        .findByIdAndIsDeletedFalse(userId)
        .orElseThrow(() -> ExceptionUtil.badRequest(ErrorMessage.USER_NOT_FOUND));
  }

  private LocalDateTime resolveRecordedAt(LocalDateTime recordedAt) {
    return recordedAt != null ? recordedAt : LocalDateTime.now();
  }
}
