package com.eefood.reactionservice.mapper;

import com.eefood.reactionservice.dto.response.PostReactionResponse;
import com.eefood.reactionservice.model.PostReaction;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PostReactionMapper {

  @Mapping(source = "post.id", target = "postId")
  PostReactionResponse toResponse(PostReaction reaction);
}