package com.eefood.iamservice.service;

import com.eefood.iamservice.dto.response.UserResponseDto;
import com.eefood.iamservice.model.User;
import com.eefood.iamservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {
  private final UserRepository userRepository;

  public UserResponseDto getCurrentUser() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    String authId = authentication.getName(); // authId trong JWT

    User user = userRepository.findByAuthId(authId)
      .orElseThrow(() -> new RuntimeException("User not found"));

    return new UserResponseDto(
      user.getId(),
      user.getUsername(),
      user.getEmail(),
      user.getRole().name(),
      user.getDob(),
      user.getGender() != null ? user.getGender().name() : null,
      user.getAddress(),
      user.getProvider().name(),
      user.getAvatarUrl(),
      user.getAllergies(),
      user.getEatingPreferences()
    );
  }
}
