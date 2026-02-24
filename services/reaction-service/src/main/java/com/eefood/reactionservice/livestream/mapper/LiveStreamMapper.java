package com.eefood.reactionservice.livestream.mapper;

import com.eefood.reactionservice.livestream.dto.response.LiveStreamResponse;
import com.eefood.reactionservice.livestream.model.LiveStream;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface LiveStreamMapper {
  LiveStreamResponse toResponse(LiveStream liveStream);
}
