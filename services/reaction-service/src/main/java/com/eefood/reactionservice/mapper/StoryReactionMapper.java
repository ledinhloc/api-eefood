package com.eefood.reactionservice.mapper;

import com.eefood.reactionservice.dto.response.StoryReactionResponse;
import com.eefood.reactionservice.model.StoryReaction;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface StoryReactionMapper {
    @Mapping(target = "storyId", source = "story.id")
    StoryReactionResponse toResponse(StoryReaction storyReaction);
}
