package com.eefood.iamservice.mapper;

import com.eefood.iamservice.dto.request.UserCreateRequest;
import com.eefood.iamservice.dto.request.UserUpdateRequest;
import com.eefood.iamservice.dto.response.UserResponse;
import com.eefood.iamservice.model.User;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring")
public interface UserMapper {
  UserResponse toUserResponse(User user);

  User toUser(UserCreateRequest request);

  @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
  void updateUserFromRequest(UserUpdateRequest request, @MappingTarget User user);
}
