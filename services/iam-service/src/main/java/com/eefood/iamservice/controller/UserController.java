package com.eefood.iamservice.controller;

import com.eefood.iamservice.dto.request.UserCreateRequest;
import com.eefood.iamservice.dto.response.ResponseData;
import com.eefood.iamservice.dto.response.UserResponse;
import com.eefood.iamservice.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users")
public class UserController {
  private final UserService userService;

  @GetMapping("/me")
  public ResponseData<UserResponse> getMe() {
    return new ResponseData<>(HttpStatus.OK.value(), "Success", userService.getCurrentUser()) ;
  }

  @PostMapping
  public ResponseData<UserResponse> createUser(@RequestBody @Valid UserCreateRequest request){
    return new ResponseData<>(HttpStatus.OK.value(), "Create Success", userService.createUser(request));
  }
}
