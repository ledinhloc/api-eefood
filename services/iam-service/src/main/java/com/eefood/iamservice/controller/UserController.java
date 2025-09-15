package com.eefood.iamservice.controller;

import com.eefood.iamservice.dto.request.AdminUpdateRequest;
import com.eefood.iamservice.dto.request.UserCreateRequest;
import com.eefood.iamservice.dto.request.UserUpdateRequest;
import com.eefood.iamservice.dto.response.ResponseData;
import com.eefood.iamservice.dto.response.UserResponse;
import com.eefood.iamservice.dto.response.UserUpdateResponse;
import com.eefood.iamservice.enums.ErrorMessage;
import com.eefood.iamservice.enums.SuccessMessage;
import com.eefood.iamservice.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
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

  @PreAuthorize("hasRole('ADMIN')")
  @PostMapping
  public ResponseData<UserResponse> createUser(@RequestBody @Valid UserCreateRequest request){
    return new ResponseData<>(HttpStatus.OK.value(), "Create Success", userService.createUser(request));
  }

  @PutMapping("/update")
  public ResponseData<UserResponse> updateUser(@RequestBody @Valid UserUpdateRequest request){
    UserResponse userResponse = userService.updateUser(request);
    return new ResponseData<>(HttpStatus.OK.value(), SuccessMessage.UPDATE_USER_SUCCESS.getMessage(), userResponse);
  }

  @PreAuthorize("hasRole('ADMIN')")
  @PutMapping("/update-role")
  public ResponseData<UserResponse> updateRoleUser(@RequestBody @Valid AdminUpdateRequest request){
    UserResponse userResponse = userService.updateRole(request.getId(), request.getRole());
    return new ResponseData<>(HttpStatus.OK.value(), SuccessMessage.UPDATE_ROLE_OF_USER_SUCCESS.getMessage(), userResponse);
  }

  @PreAuthorize("hasRole('ADMIN')")
  @DeleteMapping("/delete/{id}")
  public ResponseData<Void> deleteUser(@PathVariable Long id){
    userService.softDeleteUser(id);
    return new ResponseData<>(HttpStatus.OK.value(), SuccessMessage.DELETE_USER_SUCCESS.getMessage(), null);
  }

}
