package com.eefood.iamservice.service;

import com.eefood.iamservice.dto.request.Credential;
import com.eefood.iamservice.dto.request.TokenExchangeParam;
import com.eefood.iamservice.dto.request.UserCreateRequest;
import com.eefood.iamservice.dto.request.UserCreationParam;
import com.eefood.iamservice.dto.response.UserResponse;
import com.eefood.iamservice.mapper.UserMapper;
import com.eefood.iamservice.model.User;
import com.eefood.iamservice.repository.UserRepository;
import com.eefood.iamservice.repository.httpclient.KeycloakClient;
import lombok.RequiredArgsConstructor;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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
  private final KeycloakClient keycloakClient;

  @Value("${idp.realm}")
  @NonFinal
  String realm;

  @Value("${idp.client-id}")
  @NonFinal
  String clientId;

  @Value("${idp.client-secret}")
  @NonFinal
  String clientSecret;

  @Value("${idp.realm}")
  @NonFinal
  private String keycloakRealm;

  //lay user dang login
  public UserResponse getCurrentUser() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    String authId = authentication.getName(); // authId trong JWT

    User user = userRepository.findByAuthIdAndIsDeletedFalse(authId)
      .orElseThrow(() -> new RuntimeException("User not found"));

    return userMapper.toUserResponse(user);
  }

  @Transactional
  public UserResponse createUser(UserCreateRequest request) {
    //kiem tra email chua ton tai
    Optional<User> userOpt = userRepository.findByEmail(request.getEmail());
    if(userOpt.isPresent()) {
      throw new RuntimeException("User already exists");
    }

    //luu user
    User user = userMapper.toUser(request);
    User savedUser = userRepository.save(user);

    // Get token from keycloak
    var token = keycloakClient.exchangeToken(
      realm,
      TokenExchangeParam.builder()
        .grant_type("client_credentials")
        .client_id(clientId)
        .client_secret(clientSecret)
        .scope("openid")
        .build());

    //Create user in Keycloak
    var creationResponse = keycloakClient.createUser(
      "Bearer " + token.getAccessToken(),
      realm,
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
}
