package com.eefood.iamservice.service;

import com.eefood.iamservice.dto.request.Credential;
import com.eefood.iamservice.dto.request.UserCreateRequest;
import com.eefood.iamservice.dto.request.UserCreationParam;
import com.eefood.iamservice.dto.request.UserUpdateRequest;
import com.eefood.iamservice.dto.response.UserNotificationResponse;
import com.eefood.iamservice.dto.response.UserResponse;
import com.eefood.iamservice.enums.ErrorMessage;
import com.eefood.iamservice.enums.Role;
import com.eefood.iamservice.mapper.UserMapper;
import com.eefood.iamservice.model.User;
import com.eefood.iamservice.repository.UserRepository;
import com.eefood.iamservice.utils.ExceptionUtil;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
@Slf4j
public class UserService {
  private final UserRepository userRepository;
  private final UserMapper userMapper;
  private final KeycloakAdminService keycloakAdminService;

  // lay user dang login
  public UserResponse getCurrentUser() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    String authId = authentication.getName(); // authId trong JWT

    User user =
        userRepository
            .findByAuthIdAndIsDeletedFalse(authId)
            .orElseThrow(() -> ExceptionUtil.badRequest(ErrorMessage.USER_NOT_FOUND));

    return userMapper.toUserResponse(user);
  }

  @Transactional
  public UserResponse createUser(UserCreateRequest request) {
    // kiem tra email chua ton tai
    Optional<User> userOpt = userRepository.findByEmail(request.getEmail());
    if (userOpt.isPresent()) {
      throw ExceptionUtil.badRequest(ErrorMessage.USER_EXISTED);
    }

    // luu user
    User user = userMapper.toUser(request);
    User savedUser = userRepository.save(user);

    // Create user in Keycloak
    var creationResponse =
        keycloakAdminService.createUserInKeycloak(
            UserCreationParam.builder()
                .email(request.getEmail())
                .enabled(true)
                .emailVerified(true)
                .credentials(
                    List.of(
                        Credential.builder()
                            .type("password")
                            .temporary(false)
                            .value(request.getPassword())
                            .build()))
                .build());

    // Extract userId
    String authId = extractUserId(creationResponse);

    // gan idKeycloak va luu lai lan nua
    savedUser.setAuthId(authId);
    userRepository.save(savedUser);
    // luu user
    return userMapper.toUserResponse(savedUser);
  }

  // Hàm cập nhật và kích hoạt user
  @Transactional
  public UserResponse updateUser(UserUpdateRequest request) {
    try {
      User user =
          userRepository
              .findByIdAndIsDeletedFalse(request.getId())
              .orElseThrow(() -> ExceptionUtil.badRequest(ErrorMessage.USER_NOT_FOUND));
      // neu thay doi email
      if (request.getEmail() != null && !request.getEmail().equals(user.getEmail())) {
        keycloakAdminService.updateUserInKeycloak(
            user.getAuthId(), Map.of("email", request.getEmail()));
      }
      userMapper.updateUserFromRequest(request, user);
      User updateUser = userRepository.save(user);
      return userMapper.toUserResponse(updateUser);
    } catch (Exception e) {
      throw ExceptionUtil.badRequest(ErrorMessage.FAIL_UPDATE_USER);
    }
  }

  @Transactional
  public UserResponse updateRole(Long userId, Role role) {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    String authId = authentication.getName();

    User user =
        userRepository
            .findByIdAndIsDeletedFalse(userId)
            .orElseThrow(() -> ExceptionUtil.badRequest(ErrorMessage.USER_NOT_EXISTED));

    if (authId == null || authId.isBlank()) {
      authId =
          keycloakAdminService
              .findUserIdByEmail(user.getEmail())
              .orElseThrow(() -> ExceptionUtil.badRequest(ErrorMessage.USER_NOT_FOUND));
    }

    Role oldRole = user.getRole();

    // Cập nhật role trong DB
    user.setRole(role);
    userRepository.save(user);

    try {
      // Nếu có old role khác new role -> remove old role
      if (oldRole != null && !oldRole.equals(role)) {
        keycloakAdminService.removeRealmRoleFromUser(authId, oldRole.name());
      }

      // Gán role mới (nếu chưa có)
      keycloakAdminService.assignRealmRoleToUser(authId, role.name());
    } catch (Exception ex) {
      log.error(
          "Failed to sync role to Keycloak for userId={}, kcId={}, newRole={}",
          userId,
          authId,
          role,
          ex);
      throw ExceptionUtil.badRequest(ErrorMessage.FAIL_UPDATE_ROLE);
    }

    return userMapper.toUserResponse(user);
  }

  // Hàm xóa mềm user
  @Transactional
  public void softDeleteUser(Long userId) {
    User user =
        userRepository
            .findByIdAndIsDeletedFalse(userId)
            .orElseThrow(() -> ExceptionUtil.badRequest(ErrorMessage.USER_NOT_FOUND));

    user.setIsDeleted(true);
    userRepository.save(user);

    keycloakAdminService.disableUserInKeycloak(user.getAuthId());
  }

  private String extractUserId(ResponseEntity<?> response) {
    String location = response.getHeaders().get("Location").get(0);
    String[] splitedStr = location.split("/");
    return splitedStr[splitedStr.length - 1];
  }

  public UserResponse login(String email, String password) {
    var response = keycloakAdminService.login(email, password);

    Optional<User> userOpt = userRepository.findByEmailAndIsDeletedFalse(email);
    if (userOpt.isEmpty()) {
      throw ExceptionUtil.badRequest(ErrorMessage.USER_NOT_FOUND);
    }

    var userResponse = userMapper.toUserResponse(userOpt.get());
    // gan access token, refresh token
    userResponse.setAccessToken(response.getAccessToken());
    userResponse.setRefreshToken(response.getRefreshToken());
    return userResponse;
  }

  public List<UserNotificationResponse> getUserForNotifications() {
    List<User> response = userRepository.findAllByIsDeletedFalse();
    return response.stream().map(userMapper::toUserNotificationResponse).collect(Collectors.toList());
  }
}
