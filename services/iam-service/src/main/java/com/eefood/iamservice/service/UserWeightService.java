package com.eefood.iamservice.service;

import com.eefood.iamservice.dto.request.UserWeightRequest;
import com.eefood.iamservice.dto.response.UserWeightResponse;
import com.eefood.iamservice.enums.ErrorMessage;
import com.eefood.iamservice.mapper.UserBodyMapper;
import com.eefood.iamservice.model.User;
import com.eefood.iamservice.model.UserWeight;
import com.eefood.iamservice.repository.UserRepository;
import com.eefood.iamservice.repository.UserWeightRepository;
import com.eefood.iamservice.utils.ExceptionUtil;
import com.eefood.iamservice.utils.SecurityUtil;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserWeightService {
  private final UserWeightRepository userWeightRepository;
  private final UserRepository userRepository;
  private final SecurityUtil securityUtil;
  private final UserBodyMapper userBodyMapper;

  public List<UserWeightResponse> getMyWeights() {
    Long userId = securityUtil.getCurrentUserId();
    return userWeightRepository.findAllByUser_IdOrderByRecordedDateDesc(userId).stream()
        .map(userBodyMapper::toWeightResponse)
        .toList();
  }

  @Transactional
  public UserWeightResponse createMyWeight(UserWeightRequest request) {
    User user = getCurrentUser();
    LocalDate recordedDate = resolveRecordedDate(request.getRecordedDate());
    boolean exists =
        userWeightRepository.existsByUser_IdAndRecordedDate(user.getId(), recordedDate);
    if (exists) {
      throw ExceptionUtil.badRequest(ErrorMessage.USER_WEIGHT_ALREADY_EXISTS_IN_DAY);
    }

    UserWeight userWeight =
        UserWeight.builder()
            .user(user)
            .weightKg(request.getWeightKg())
            .recordedDate(recordedDate)
            .build();

    return userBodyMapper.toWeightResponse(userWeightRepository.save(userWeight));
  }

  @Transactional
  public UserWeightResponse updateMyWeight(Long weightId, UserWeightRequest request) {
    Long userId = securityUtil.getCurrentUserId();
    UserWeight userWeight =
        userWeightRepository
            .findByIdAndUser_Id(weightId, userId)
            .orElseThrow(() -> ExceptionUtil.notFound(ErrorMessage.USER_WEIGHT_NOT_FOUND));

    userWeight.setWeightKg(request.getWeightKg());
    if (request.getRecordedDate() != null) {
      boolean exists =
          userWeightRepository.existsByUser_IdAndRecordedDateAndIdNot(
              userId, request.getRecordedDate(), weightId);
      if (exists) {
        throw ExceptionUtil.badRequest(ErrorMessage.USER_WEIGHT_ALREADY_EXISTS_IN_DAY);
      }
      userWeight.setRecordedDate(request.getRecordedDate());
    }

    return userBodyMapper.toWeightResponse(userWeightRepository.save(userWeight));
  }

  @Transactional
  public void deleteMyWeight(Long weightId) {
    Long userId = securityUtil.getCurrentUserId();
    UserWeight userWeight =
        userWeightRepository
            .findByIdAndUser_Id(weightId, userId)
            .orElseThrow(() -> ExceptionUtil.notFound(ErrorMessage.USER_WEIGHT_NOT_FOUND));

    userWeightRepository.delete(userWeight);
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
