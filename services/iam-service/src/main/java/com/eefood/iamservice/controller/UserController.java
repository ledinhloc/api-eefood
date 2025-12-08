package com.eefood.iamservice.controller;

import com.eefood.iamservice.dto.request.AdminUpdateRequest;
import com.eefood.iamservice.dto.request.UserCreateRequest;
import com.eefood.iamservice.dto.request.UserUpdateRequest;
import com.eefood.iamservice.dto.response.ResponseData;
import com.eefood.iamservice.dto.response.UserInfo;
import com.eefood.iamservice.dto.response.UserNotificationResponse;
import com.eefood.iamservice.dto.response.UserResponse;
import com.eefood.iamservice.enums.Provider;
import com.eefood.iamservice.enums.Role;
import com.eefood.iamservice.enums.SuccessMessage;
import com.eefood.iamservice.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users")
public class UserController {
  private final UserService userService;

  @PreAuthorize("hasRole('ADMIN')")
  @GetMapping
  public ResponseData<Page<UserResponse>> getUsers(
          @RequestParam(defaultValue = "0") int page,
          @RequestParam(defaultValue = "10") int size,
          @RequestParam(defaultValue = "") String search,
          @RequestParam(required = false) Role role,
          @RequestParam(required = false) Provider provider,
          @RequestParam(defaultValue = "username") String sortBy,
          @RequestParam(defaultValue = "asc") String direction
  ) {
    Sort sort = Sort.by(direction.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC, sortBy);
    Pageable pageable = PageRequest.of(page, size, sort);
    Page<UserResponse> users = userService.getUsers(search, role, provider, pageable);
    return new ResponseData<>(200, "Success", users);
  }

  @GetMapping("/info/{userId}")
  public ResponseData<UserResponse> getUserById(@PathVariable Long userId) {
    if(userId == null) {
      return new ResponseData<>(HttpStatus.BAD_REQUEST.value(), "userId is null",null);
    }
    return new ResponseData<>(HttpStatus.OK.value(), "success",userService.getUserById(userId));
  }

  @DeleteMapping("/cache-user-info")
  public ResponseData<List<UserInfo>> deleteCacheUserInfo(@RequestBody List<Long> userIds) {

    if (userIds == null || userIds.isEmpty()) {
      return new ResponseData<>(HttpStatus.BAD_REQUEST.value(), "UserIds list cannot be empty", null);
    }

    userIds.forEach(userService::evictUserCache);
    return new ResponseData<>(HttpStatus.OK.value(), "Delete Success");
  }

  @GetMapping("/{userId}")
  public ResponseData<UserInfo> getUserInfo(@PathVariable Long userId) {
    UserInfo info = userService.getUserInfo(userId);
    if (info == null) {
      return new ResponseData<>(HttpStatus.NOT_FOUND.value(), "User not found", null);
    }
    return new ResponseData<>(HttpStatus.OK.value(), "Success", info);
  }

  @PostMapping("/batch")
  public ResponseData<List<UserInfo>> getUserInfoBatch(@RequestBody List<Long> userIds) {
    List<UserInfo> list =  userIds.stream()
      .map(userService::getUserInfo)
      .filter(Objects::nonNull)
      .collect(Collectors.toList());
    return new ResponseData<>(HttpStatus.OK.value(), "Success", list);
  }

  @GetMapping("/me")
  public ResponseData<UserResponse> getMe() {
    return new ResponseData<>(HttpStatus.OK.value(), "Success", userService.getCurrentUser());
  }

  @PreAuthorize("hasRole('ADMIN')")
  @PostMapping
  public ResponseData<UserResponse> createUser(@RequestBody @Valid UserCreateRequest request) {
    return new ResponseData<>(
        HttpStatus.OK.value(), "Create Success", userService.createUser(request));
  }

  @PutMapping("/update")
  public ResponseData<UserResponse> updateUser(@RequestBody @Valid UserUpdateRequest request) {
    UserResponse userResponse = userService.updateUser(request);
    return new ResponseData<>(
        HttpStatus.OK.value(), SuccessMessage.UPDATE_USER_SUCCESS.getMessage(), userResponse);
  }

  @PreAuthorize("hasRole('ADMIN')")
  @PutMapping("/update-role")
  public ResponseData<UserResponse> updateRoleUser(@RequestBody @Valid AdminUpdateRequest request) {
    UserResponse userResponse = userService.updateRole(request.getId(), request.getRole());
    return new ResponseData<>(
        HttpStatus.OK.value(),
        SuccessMessage.UPDATE_ROLE_OF_USER_SUCCESS.getMessage(),
        userResponse);
  }

  @PreAuthorize("hasRole('ADMIN')")
  @DeleteMapping("/delete/{id}")
  public ResponseData<Void> deleteUser(@PathVariable Long id) {
    userService.softDeleteUser(id);
    return new ResponseData<>(
        HttpStatus.OK.value(), SuccessMessage.DELETE_USER_SUCCESS.getMessage(), null);
  }

  @GetMapping("/all")
  public ResponseData<List<UserNotificationResponse>> getAllUsers() {
    return new ResponseData<>(HttpStatus.OK.value(), "Success", userService.getUserForNotifications());
  }
}
