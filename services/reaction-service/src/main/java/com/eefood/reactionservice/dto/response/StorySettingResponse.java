package com.eefood.reactionservice.dto.response;

import com.eefood.reactionservice.enums.StoryMode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StorySettingResponse {
    private Long id;
    private Long userId;
    private StoryMode mode;
    private List<Long> allowedUserIds;
    private List<Long> blockedUserIds;
}
