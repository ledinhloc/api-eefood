package com.eefood.reactionservice.mapper;

import com.eefood.reactionservice.dto.response.ApprovePostResponse;
import com.eefood.reactionservice.model.ApprovePost;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ApprovePostMapper {

  @Mapping(source = "post.id", target = "postId")
  ApprovePostResponse toResponse(ApprovePost approvePost);
}
