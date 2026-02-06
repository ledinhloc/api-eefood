package com.eefood.reactionservice.livestream.mapper;

import com.eefood.reactionservice.livestream.dto.response.LiveReactionResponse;
import com.eefood.reactionservice.livestream.model.LiveReaction;
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
