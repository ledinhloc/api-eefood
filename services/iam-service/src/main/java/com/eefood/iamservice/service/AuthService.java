package com.eefood.iamservice.service;

import com.eefood.iamservice.dto.request.*;
import com.eefood.iamservice.dto.response.UserResponse;
import com.eefood.iamservice.enums.ErrorMessage;
import com.eefood.iamservice.enums.OtpType;
import com.eefood.iamservice.enums.Provider;
import com.eefood.iamservice.enums.Role;
import com.eefood.iamservice.mapper.UserMapper;
import com.eefood.iamservice.model.Otp;
import com.eefood.iamservice.model.User;
import com.eefood.iamservice.repository.OtpRepository;
import com.eefood.iamservice.repository.UserRepository;
import com.eefood.iamservice.utils.ExceptionUtil;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
@Slf4j
public class AuthService {
  private final UserRepository userRepository;
  private final OtpRepository otpRepository;
  private final UserMapper userMapper;
  private final OtpService otpService;
  private final KeycloakAdminService keycloakAdminService;

  // Hàm đăng ký user
  @Transactional
  public UserResponse registerUser(UserSignUpRequest request) {
    Optional<User> userOtp = userRepository.findByEmail(request.getEmail());

    // Kiểm tra user có tồn tại?
    if (userOtp.isPresent()) {
      // Có tồn tại
      User existingUser = userOtp.get();
      if (Boolean.FALSE.equals(existingUser.getIsDeleted())) {
        // user đã active
        throw ExceptionUtil.badRequest(ErrorMessage.USER_EXISTED);
      }

      // Giới hạn số lần gửi mail
      if (!otpService.canSendOtp(existingUser)) {
        log.warn(String.format("OTP send to much"));
        throw ExceptionUtil.badRequest(ErrorMessage.OTP_SEND_TO_MUCH);
      }

      // user tồn tại nhưng chưa active → gửi lại OTP, không tạo user mới
      otpService.sendOtp(request.getEmail(), OtpType.REGISTER);
      log.info(String.format("Registered user: %s", request.getEmail()));
      return userMapper.toUserResponse(existingUser);
    }

    // Kiểm tra user tồn tại trên Keycloak chưa
    Optional<String> keycloakUserIdOpt = keycloakAdminService.findUserIdByEmail(request.getEmail());
    if (keycloakUserIdOpt.isPresent()) {
      log.warn("User exists in Keycloak: {}", request.getEmail());
      throw ExceptionUtil.badRequest(ErrorMessage.USER_EXISTED);
    }

    // user chưa tồn tại → tạo mới
    User user =
        User.builder()
            .email(request.getEmail())
            .username(request.getUsername())
            .provider(Provider.NORMAL)
            .role(request.getRole()!=null ? Role.ADMIN : Role.USER)
            .isDeleted(true)
            .build();
    User savedUser = userRepository.save(user);

    // Create user in Keycloak
    var creationResponse =
        keycloakAdminService.createUserInKeycloak(
            UserCreationParam.builder()
                .email(request.getEmail())
                .enabled(false)
                .emailVerified(false)
                .credentials(
                    List.of(
                        Credential.builder()
                            .type("password")
                            .temporary(false)
                            .value(request.getPassword())
                            .build()))
                .attributes(Map.of("userId", String.valueOf(savedUser.getId())))
                .build());

    handleKeycloakCreationResponse(creationResponse, savedUser, request);

    // Giới hạn số lần gửi mail
    if (!otpService.canSendOtp(savedUser)) {
      log.warn(String.format("OTP send to much"));
      throw ExceptionUtil.badRequest(ErrorMessage.OTP_SEND_TO_MUCH);
    }

    log.info(String.format("Registered user: %s", request.getEmail()));
    otpService.sendOtp(request.getEmail(), OtpType.REGISTER);
    return userMapper.toUserResponse(savedUser);
  }

  // Hàm xác thực otp
  @Transactional
  public boolean verifyOtp(String email, String otpCode, OtpType otpType) {
    // Tìm kiếm user theo email
    User user =
        userRepository
            .findByEmail(email)
            .orElseThrow(() -> ExceptionUtil.badRequest(ErrorMessage.USER_NOT_FOUND));
    // Hàm tìm otp theo user + otpNum
    Optional<Otp> otpOptional = otpRepository.findByUserAndOtpNumAndIsDeletedFalse(user, otpCode);
    if (otpOptional.isEmpty()) return false;

    Otp otp = otpOptional.get();

    // Kiểm tra otp hết hạn
    if (otp.getOtpExpired().isBefore(LocalDateTime.now())) {
      otp.setIsDeleted(true);
      otpRepository.save(otp);
      return false;
    }

    otp.setIsDeleted(true);
    otpRepository.save(otp);

    if (otpType.equals(OtpType.REGISTER)) {
      user.setIsDeleted(false);
      userRepository.save(user);
    }

    // Kích hoạt user keycloak
    boolean kcEnabled = enableUser(user, email);

    return kcEnabled;
  }

  public void resetPassword(String email, String newPassword) {
    User user =
        userRepository
            .findByEmailAndIsDeletedFalse(email)
            .orElseThrow(() -> ExceptionUtil.badRequest(ErrorMessage.USER_NOT_FOUND));
    String keycloakId = user.getAuthId();
    keycloakAdminService.resetPassword(keycloakId, newPassword);
  }

  // Hàm kích hoạt user keycloak
  private boolean enableUser(User user, String email) {
    boolean kcEnabled = false;
    String keycloakId = user.getAuthId();
    if (keycloakId != null && !keycloakId.isBlank()) {
      // Kích hoạt user
      kcEnabled = keycloakAdminService.enableUserInKeycloak(keycloakId);
    } else {

      var maybeId = keycloakAdminService.findUserIdByEmail(email);
      if (maybeId.isPresent()) {
        // Kích hoạt user
        kcEnabled = keycloakAdminService.enableUserInKeycloak(maybeId.get());
      } else {
        log.warn("User {} not found in Keycloak while verifying OTP", email);
        kcEnabled = false;
      }
    }
    return kcEnabled;
  }

  // Hàm xử lý trạng thái keycloak
  private void handleKeycloakCreationResponse(
      ResponseEntity<?> creationResponse, User savedUser, UserSignUpRequest request) {
    if (creationResponse == null) {
      throw new RuntimeException(
          "Keycloak createUser response is null for email=" + request.getEmail());
    }

    HttpStatusCode status = creationResponse.getStatusCode();

    if (status == HttpStatus.CREATED) {
      String authId = extractUserId(creationResponse);
      if (authId != null) {
        savedUser.setAuthId(authId);
        userRepository.save(savedUser);
        log.info("Created keycloak user for email={} authId={}", request.getEmail(), authId);
      } else {
        // Không nhận được Location header -> log warning
        log.warn(
            "Keycloak returned CREATED but Location header missing for email={}",
            request.getEmail());
      }
    } else if (status == HttpStatus.CONFLICT) {
      // user đã tồn tại trên Keycloak (có thể do race condition)
      log.warn("Keycloak reported conflict when creating user with email={}", request.getEmail());
    } else {
      // các trạng thái khác -> log & ném ngoại lệ nếu cần
      throw new RuntimeException(
          "Failed to create user in Keycloak. status="
              + status
              + " for email="
              + request.getEmail());
    }
  }

  private String extractUserId(ResponseEntity<?> response) {
    String location = response.getHeaders().get("Location").get(0);
    String[] splitedStr = location.split("/");
    return splitedStr[splitedStr.length - 1];
  }
}
