package com.eefood.reactionservice.livestream.mapper;

import com.eefood.reactionservice.dto.response.UserInfo;
import com.eefood.reactionservice.livestream.dto.response.BlockUserResponse;
import com.eefood.reactionservice.livestream.model.LiveStreamBlock;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface LiveStreamBlockMapper {

  @Mapping(target = "blockedUserId", source = "block.blockedUserId")
  @Mapping(target = "createdAt", source = "block.createdAt")
  @Mapping(target = "username", source = "info.username")
  @Mapping(target = "avatarUrl", source = "info.avatarUrl")
  @Mapping(target = "email", source = "info.email")
  BlockUserResponse toResponse(LiveStreamBlock block, UserInfo info);
}
