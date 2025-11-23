package com.eefood.reactionservice.mapper;

import com.eefood.reactionservice.dto.response.LiveStreamResponse;
import com.eefood.reactionservice.model.livestream.LiveStream;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface LiveStreamMapper {
  LiveStreamResponse toResponse(LiveStream liveStream);
}
