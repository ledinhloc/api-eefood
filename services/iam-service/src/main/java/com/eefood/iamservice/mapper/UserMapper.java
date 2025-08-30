package com.eefood.iamservice.mapper;

import com.eefood.iamservice.dto.request.UserCreateRequest;
import com.eefood.iamservice.dto.request.UserSignUpRequest;
import com.eefood.iamservice.dto.response.UserResponse;
import com.eefood.iamservice.model.User;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {
  UserResponse toUserResponse(User user);
  User toUser(UserCreateRequest request);
}
