package com.eefood.reactionservice.mapper;

import com.eefood.reactionservice.dto.response.StoryViewResponse;
import com.eefood.reactionservice.model.StoryView;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface StoryViewMapper {
    StoryViewResponse toResponse(StoryView view);
}
