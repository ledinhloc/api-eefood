package com.eefood.reactionservice.livestream.mapper;

import com.eefood.reactionservice.livestream.dto.response.LiveGiftItemResponse;
import com.eefood.reactionservice.livestream.model.LiveGiftItem;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface LiveGiftMapper {
    LiveGiftItemResponse toResponse(LiveGiftItem liveGiftItem);
}
