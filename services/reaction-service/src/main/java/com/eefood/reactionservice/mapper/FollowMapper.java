package com.eefood.reactionservice.mapper;

import com.eefood.reactionservice.dto.response.FollowResponse;
import com.eefood.reactionservice.model.Follow;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface FollowMapper {
    FollowResponse toResponse(Follow follow);
}
