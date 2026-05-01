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
import java.time.LocalDate;
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
    return userHeightRepository.findAllByUser_IdOrderByRecordedDateDesc(userId).stream()
        .map(userBodyMapper::toHeightResponse)
        .toList();
  }

  @Transactional
  public UserHeightResponse createMyHeight(UserHeightRequest request) {
    User user = getCurrentUser();
    LocalDate recordedDate = resolveRecordedDate(request.getRecordedDate());
    boolean exists =
        userHeightRepository.existsByUser_IdAndRecordedDate(user.getId(), recordedDate);
    if (exists) {
      throw ExceptionUtil.badRequest(ErrorMessage.USER_HEIGHT_ALREADY_EXISTS_IN_DAY);
    }

    UserHeight userHeight =
        UserHeight.builder()
            .user(user)
            .heightCm(request.getHeightCm())
            .recordedDate(recordedDate)
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
    if (request.getRecordedDate() != null) {
      boolean exists =
          userHeightRepository.existsByUser_IdAndRecordedDateAndIdNot(
              userId, request.getRecordedDate(), heightId);
      if (exists) {
        throw ExceptionUtil.badRequest(ErrorMessage.USER_HEIGHT_ALREADY_EXISTS_IN_DAY);
      }
      userHeight.setRecordedDate(request.getRecordedDate());
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

  private LocalDate resolveRecordedDate(LocalDate recordedDate) {
    return recordedDate != null ? recordedDate : LocalDate.now();
  }
}
