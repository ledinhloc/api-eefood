package com.eefood.reactionservice.mapper;

import com.eefood.reactionservice.dto.response.LiveReactionResponse;
import com.eefood.reactionservice.model.livestream.LiveReaction;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface LiveReactionMapper {

  LiveReactionMapper INSTANCE = Mappers.getMapper(LiveReactionMapper.class);

  @Mapping(source = "liveStream.id", target = "liveStreamId")
  @Mapping(target = "username", ignore = true)
  @Mapping(target = "avatarUrl", ignore = true)
  LiveReactionResponse toResponse(LiveReaction reaction);
}
