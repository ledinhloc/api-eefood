package com.eefood.iamservice.service;

import com.eefood.iamservice.dto.request.Credential;
import com.eefood.iamservice.dto.request.UserCreateRequest;
import com.eefood.iamservice.dto.request.UserCreationParam;
import com.eefood.iamservice.dto.response.UserResponse;
import com.eefood.iamservice.enums.ErrorMessage;
import com.eefood.iamservice.mapper.UserMapper;
import com.eefood.iamservice.model.User;
import com.eefood.iamservice.repository.UserRepository;
import com.eefood.iamservice.utils.ExceptionUtil;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
@Service
@Slf4j
public class UserService {
  private final UserRepository userRepository;
  private final UserMapper userMapper;
  private final KeycloakAdminService keycloakAdminService;

  //lay user dang login
  public UserResponse getCurrentUser() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    String authId = authentication.getName(); // authId trong JWT

    User user = userRepository.findByAuthIdAndIsDeletedFalse(authId)
      .orElseThrow(()-> ExceptionUtil.badRequest(ErrorMessage.USER_NOT_FOUND));

    return userMapper.toUserResponse(user);
  }

  @Transactional
  public UserResponse createUser(UserCreateRequest request) {
    //kiem tra email chua ton tai
    Optional<User> userOpt = userRepository.findByEmail(request.getEmail());
    if(userOpt.isPresent()) {
      throw ExceptionUtil.badRequest(ErrorMessage.USER_EXISTED);
    }

    //luu user
    User user = userMapper.toUser(request);
    User savedUser = userRepository.save(user);

    //Create user in Keycloak
    var creationResponse = keycloakAdminService.createUserInKeycloak(
            UserCreationParam.builder()
                    .email(request.getEmail())
                    .enabled(true)
                    .emailVerified(true)
                    .credentials(List.of(Credential.builder()
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
    //luu user
    return userMapper.toUserResponse(savedUser);
  }

  private String extractUserId(ResponseEntity<?> response) {
    String location = response.getHeaders().get("Location").get(0);
    String[] splitedStr = location.split("/");
    return splitedStr[splitedStr.length - 1];
  }

  public UserResponse login(String email, String password) {
    var response = keycloakAdminService.login(email, password);

    Optional<User> userOpt = userRepository.findByEmailAndIsDeletedFalse(email);
    if(userOpt.isEmpty()){
      throw ExceptionUtil.badRequest(ErrorMessage.USER_NOT_FOUND);
    }

    var userResponse = userMapper.toUserResponse(userOpt.get());
    // gan access token, refresh token
    userResponse.setAccessToken(response.getAccessToken());
    userResponse.setRefreshToken(response.getRefreshToken());
    return userResponse;
  }
}
