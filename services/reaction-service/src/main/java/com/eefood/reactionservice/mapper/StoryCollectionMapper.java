package com.eefood.reactionservice.mapper;

import com.eefood.reactionservice.dto.response.StoryCollectionResponse;
import com.eefood.reactionservice.model.StoryCollection;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface StoryCollectionMapper {
    StoryCollectionResponse toResponse(StoryCollection storyCollection);
}
