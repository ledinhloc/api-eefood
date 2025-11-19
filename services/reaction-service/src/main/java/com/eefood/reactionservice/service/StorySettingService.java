package com.eefood.reactionservice.service;

import com.eefood.reactionservice.enums.StoryMode;
import com.eefood.reactionservice.model.StorySetting;
import com.eefood.reactionservice.repository.StorySettingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class StorySettingService {
    private final StorySettingRepository repository;
    private final FollowService followService;

    public boolean canViewStory(Long viewerId, Long ownerId) {
        if (viewerId.equals(ownerId)) return true;

        StorySetting setting = repository.findByUserId(ownerId).orElse(null);

        if (setting == null) {
            return true;
        }

        StoryMode mode = setting.getMode();
        if (mode == null) {
            return true;
        }

        return switch (setting.getMode()) {

            case PRIVATE -> false;

            case FOLLOWING_ONLY -> {
                boolean isFollowing = followService.checkFollow(viewerId);
                yield isFollowing;
            }

            case CUSTOM_INCLUDE ->
                    setting.getAllowedUserIds() != null &&
                    setting.getAllowedUserIds().contains(viewerId);

            case BLACKLIST ->
                    setting.getBlockedUserIds() == null ||
                    !setting.getBlockedUserIds().contains(viewerId);
        };
    }
}
