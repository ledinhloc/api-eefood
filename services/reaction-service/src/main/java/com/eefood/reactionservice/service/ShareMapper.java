package com.eefood.reactionservice.service;

import com.eefood.reactionservice.dto.response.ShareResponse;
import com.eefood.reactionservice.model.Share;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ShareMapper {
    @Mapping(target = "content", source = "post.content")
    @Mapping(target = "imageUrl", source = "post.imageUrl")
    ShareResponse toResponse(Share share);
}
