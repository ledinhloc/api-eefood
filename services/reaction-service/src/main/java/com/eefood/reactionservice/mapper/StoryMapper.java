package com.eefood.reactionservice.mapper;

import com.eefood.reactionservice.dto.request.StoryRequest;
import com.eefood.reactionservice.dto.response.StoryResponse;
import com.eefood.reactionservice.model.Story;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface StoryMapper {
    Story toStory(StoryRequest request);
    StoryResponse toStoryResponse(Story story);
}
