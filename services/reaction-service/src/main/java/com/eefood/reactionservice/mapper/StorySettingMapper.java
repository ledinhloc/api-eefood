package com.eefood.reactionservice.mapper;

import com.eefood.reactionservice.dto.response.StorySettingResponse;
import com.eefood.reactionservice.model.StorySetting;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface StorySettingMapper {
    StorySettingResponse toResponse(StorySetting storySetting);
}
