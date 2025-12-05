package com.eefood.iamservice.mapper;

import com.eefood.iamservice.dto.request.UserCreateRequest;
import com.eefood.iamservice.dto.request.UserUpdateRequest;
import com.eefood.iamservice.dto.response.UserInfo;
import com.eefood.iamservice.dto.response.UserNotificationResponse;
import com.eefood.iamservice.dto.response.UserResponse;
import com.eefood.iamservice.model.User;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface UserMapper {
  @Mapping(target = "createdAt", source = "createdAt")
  UserResponse toUserResponse(User user);

  User toUser(UserCreateRequest request);

  @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
  void updateUserFromRequest(UserUpdateRequest request, @MappingTarget User user);

  UserNotificationResponse toUserNotificationResponse(User user);
  UserInfo toResponse(User user);
}