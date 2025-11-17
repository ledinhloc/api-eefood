package com.eefood.reactionservice.dto.request;

import com.eefood.reactionservice.enums.StoryMode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class StorySettingRequest {
    private Long userId;
    private StoryMode mode;
    private List<Long> allowedUserIds;
    private List<Long> blockedUserIds;
}
