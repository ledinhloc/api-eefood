package com.eefood.reactionservice.service.story;

import com.eefood.reactionservice.dto.request.StorySettingRequest;
import com.eefood.reactionservice.dto.response.StorySettingResponse;
import com.eefood.reactionservice.enums.StoryMode;
import com.eefood.reactionservice.mapper.StorySettingMapper;
import com.eefood.reactionservice.model.StorySetting;
import com.eefood.reactionservice.repository.story.StorySettingRepository;
import com.eefood.reactionservice.service.follow.FollowService;
import com.eefood.reactionservice.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class StorySettingService {
    private final StorySettingRepository repository;
    private final FollowService followService;
    private final StorySettingMapper storySettingMapper;
    private final SecurityUtil securityUtil;

    public StorySettingResponse getSetting(Long userId) {
        StorySetting setting = repository.findByUserId(userId).orElse(null);
        if (setting == null)
            return null;

        return storySettingMapper.toResponse(setting);
    }

    public StorySettingResponse createOrUpdateSetting(StorySettingRequest request) {
        StorySetting setting = repository.findByUserId(request.getUserId()).orElse(
                StorySetting.builder()
                        .userId(request.getUserId())
                        .build()
        );

        setting.setMode(request.getMode());
        setting.setAllowedUserIds(request.getAllowedUserIds());
        setting.setBlockedUserIds(request.getBlockedUserIds());

        StorySetting saved = repository.save(setting);
        return storySettingMapper.toResponse(saved);
    }

    public void deleteSetting(Long userId) {
        repository.deleteByUserId(userId);
    }


    public boolean canViewStory(Long viewerId, Long ownerId) {
        if (viewerId.equals(ownerId)) return true;

        StorySetting setting = repository.findByUserId(ownerId).orElse(null);

        if (setting == null) {
            return followService.checkFollow(ownerId);
        }

        StoryMode mode = setting.getMode();
        if (mode == null) {
            return true;
        }

        return switch (setting.getMode()) {

            case PRIVATE -> false;

            case FOLLOWING_ONLY -> followService.checkFollow(ownerId);

            case CUSTOM_INCLUDE ->
                    setting.getAllowedUserIds() != null &&
                    setting.getAllowedUserIds().contains(viewerId);

            case BLACKLIST ->
                    setting.getBlockedUserIds() == null ||
                    !setting.getBlockedUserIds().contains(viewerId);
        };
    }
}
