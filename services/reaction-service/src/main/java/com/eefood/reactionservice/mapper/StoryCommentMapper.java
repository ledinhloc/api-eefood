package com.eefood.reactionservice.mapper;

import com.eefood.reactionservice.dto.request.StoryCommentRequest;
import com.eefood.reactionservice.dto.response.StoryCommentResponse;
import com.eefood.reactionservice.model.StoryComment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface StoryCommentMapper {
    @Mapping(source = "parentComment.id", target = "parentId")
    @Mapping(source = "story.id", target = "storyId")
    @Mapping(source = "createdAt", target = "createdAt", dateFormat = "yyyy-MM-dd HH:mm:ss")
    StoryCommentResponse toResponse(StoryComment comment);
}
