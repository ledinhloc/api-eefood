package com.eefood.reactionservice.mapper;

import com.eefood.reactionservice.dto.response.StorySettingResponse;
import com.eefood.reactionservice.model.StorySetting;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface StorySettingMapper {
    @Mapping(
            target = "allowedUserIds",
            expression = "java(storySetting.getAllowedUserIds() != null ? storySetting.getAllowedUserIds() : java.util.List.of())"
    )
    @Mapping(
            target = "blockedUserIds",
            expression = "java(storySetting.getBlockedUserIds() != null ? storySetting.getBlockedUserIds() : java.util.List.of())"
    )
    StorySettingResponse toResponse(StorySetting storySetting);
}
