package com.eefood.iamservice.controller;

import com.eefood.iamservice.dto.response.UserResponseDto;
import com.eefood.iamservice.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users")
public class UserController {
  private final UserService userService;

  @GetMapping("/me")
  public UserResponseDto getMe() {
    return userService.getCurrentUser();
  }
}
