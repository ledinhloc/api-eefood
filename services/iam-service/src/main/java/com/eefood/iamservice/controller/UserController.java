package com.eefood.iamservice.controller;

import com.eefood.iamservice.dto.request.UserCreateRequest;
import com.eefood.iamservice.dto.request.UserDeleteRequest;
import com.eefood.iamservice.dto.request.UserUpdateRequest;
import com.eefood.iamservice.dto.response.ResponseData;
import com.eefood.iamservice.dto.response.UserResponse;
import com.eefood.iamservice.enums.ErrorMessage;
import com.eefood.iamservice.enums.SuccessMessage;
import com.eefood.iamservice.service.UserService;
import jakarta.validation.Valid;
import jakarta.ws.rs.PUT;
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

  @PutMapping("/update")
  public ResponseData<UserResponse> updateUser(@RequestBody @Valid UserUpdateRequest request){
    UserResponse userResponse = userService.updateUser(request);
    if(userResponse == null){
      return new ResponseData<>(HttpStatus.NOT_FOUND.value(), ErrorMessage.FAIL_UPDATE_USER.getMessage(), null);
    }
    return new ResponseData<>(HttpStatus.OK.value(), SuccessMessage.UPDATE_USER_SUCCESS.getMessage(), userResponse);
  }

  @PutMapping("/update-profile")
  public ResponseData<UserResponse> updateProfile(@RequestBody @Valid UserUpdateRequest request){
    UserResponse userResponse = userService.updateProfileUser(request);
    if(userResponse == null){
      return new ResponseData<>(HttpStatus.NOT_FOUND.value(), ErrorMessage.FAIL_UPDATE_PROFILE_USER.getMessage(), null);
    }
    return new ResponseData<>(HttpStatus.OK.value(), SuccessMessage.UPDATE_PROFILE_USER_SUCCESS.getMessage(), userResponse);
  }

  @DeleteMapping("/delete")
  public ResponseData<Void> deleteUser(@RequestBody @Valid UserDeleteRequest request){
    userService.softDeleteUser(request.getId());
    return new ResponseData<>(HttpStatus.OK.value(), SuccessMessage.DELETE_USER_SUCCESS.getMessage(), null);
  }
}
