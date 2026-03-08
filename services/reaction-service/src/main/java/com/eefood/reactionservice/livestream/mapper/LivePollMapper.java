package com.eefood.reactionservice.livestream.mapper;

import com.eefood.reactionservice.livestream.dto.response.LivePollOptionResponse;
import com.eefood.reactionservice.livestream.dto.response.LivePollResponse;
import com.eefood.reactionservice.livestream.dto.response.LivePollSettingResponse;
import com.eefood.reactionservice.livestream.model.LivePoll;
import com.eefood.reactionservice.livestream.model.LivePollOption;
import com.eefood.reactionservice.livestream.model.LivePollSetting;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface LivePollMapper {
  @Mapping(target = "setting", ignore = true)
  @Mapping(target = "options", ignore = true)
  LivePollResponse toPollResponse(LivePoll livePoll);

  LivePollOptionResponse toOptionResponse(LivePollOption livePollOption);
  List<LivePollOptionResponse> toOptionResponses(List<LivePollOption> livePollOptions);

  LivePollSettingResponse toSettingResponse(LivePollSetting livePollSetting);

  default LivePollResponse toFullResponse(LivePoll poll, LivePollSetting setting, List<LivePollOption> options) {
    LivePollResponse res = toPollResponse(poll);
    res.setSetting(toSettingResponse(setting));
    res.setOptions(toOptionResponses(options));
    return res;
  }
}
