package com.eefood.iamservice.mapper;

import com.eefood.iamservice.dto.response.UserHeightResponse;
import com.eefood.iamservice.dto.response.UserWeightResponse;
import com.eefood.iamservice.model.UserHeight;
import com.eefood.iamservice.model.UserWeight;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserBodyMapper {
  @Mapping(target = "userId", source = "user.id")
  UserHeightResponse toHeightResponse(UserHeight userHeight);

  @Mapping(target = "userId", source = "user.id")
  UserWeightResponse toWeightResponse(UserWeight userWeight);
}
