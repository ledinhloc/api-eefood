package com.eefood.reactionservice.service.story;

import com.eefood.reactionservice.dto.request.StoryRequest;
import com.eefood.reactionservice.dto.request.StorySettingRequest;
import com.eefood.reactionservice.dto.response.StoryResponse;
import com.eefood.reactionservice.dto.response.UserInfo;
import com.eefood.reactionservice.dto.response.UserStoryResponse;
import com.eefood.reactionservice.enums.StoryMode;
import com.eefood.reactionservice.mapper.StoryMapper;
import com.eefood.reactionservice.model.Collection;
import com.eefood.reactionservice.model.Story;
import com.eefood.reactionservice.model.StorySetting;
import com.eefood.reactionservice.repository.story.StoryRepository;
import com.eefood.reactionservice.repository.story.StorySettingRepository;
import com.eefood.reactionservice.repository.story.StoryViewRepository;
import com.eefood.reactionservice.repository.httpclient.IamClient;
import com.eefood.reactionservice.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StoryService {
    private final StoryRepository storyRepository;
    private final StoryViewRepository storyViewRepository;
    private final StoryMapper storyMapper;
    private final SecurityUtil securityUtil;
    private final StorySettingService storySettingService;
    private final IamClient iamClient;
    private final StorySettingRepository storySettingRepository;

    public StoryResponse createStory(StoryRequest storyRequest) {
        StorySetting setting = storySettingRepository.findByUserId(storyRequest.getUserId()).orElse(null);
        if(setting == null) {
            storySettingService.createOrUpdateSetting(
                    StorySettingRequest.builder()
                            .mode(StoryMode.FOLLOWING_ONLY)
                            .userId(storyRequest.getUserId())
                            .allowedUserIds(Collections.emptyList())
                            .blockedUserIds(Collections.emptyList())
                            .build()
            );
        }
        Story story = Story.builder()
                .type(storyRequest.getType())
                .userId(storyRequest.getUserId())
                .contentUrl(storyRequest.getContentUrl())
                .expiredAt(LocalDateTime.now().plusHours(24))
                .build();
        storyRepository.save(story);
        return storyMapper.toStoryResponse(story);
    }

    public StoryResponse updateStory(StoryRequest storyRequest) {
        Story story = storyRepository.findByIdAndIsDeletedFalse(storyRequest.getId()).orElseThrow(() -> new RuntimeException("Story not found or deleted"));
        if (story != null) {
            story.setExpiredAt(LocalDateTime.now().plusHours(24));
            story.setContentUrl(storyRequest.getContentUrl());
            story.setType(storyRequest.getType());
            storyRepository.save(story);
        }
        return storyMapper.toStoryResponse(story);
    }

    public void softDeleteStory(Long id) {
        Story story = storyRepository.findByIdAndIsDeletedFalse(id).orElseThrow(() -> new RuntimeException("Story not found or deleted"));
        story.setIsDeleted(true);
        storyRepository.save(story);
    }

    public UserStoryResponse getOwnStory() {
        Long ownerId = securityUtil.getCurrentUserId();

        List<Story> stories = storyRepository.findAllByUserIdAndIsDeletedFalse(ownerId)
                .stream()
                .filter(s -> !isExpired(s))
                .toList();

        UserInfo info = iamClient.getUserInfo(ownerId).getData();

        List<StoryResponse> storyDtos = stories.stream()
                .map(s -> {
                    StoryResponse dto = storyMapper.toStoryResponse(s);
                    dto.setViewed(storyViewRepository.existsByStoryIdAndUserId(s.getId(), ownerId));
                    return dto;
                })
                .sorted((a, b) -> Boolean.compare(a.isViewed(), b.isViewed()))
                .toList();

        return new UserStoryResponse(
                ownerId,
                info != null ? info.getUsername() : null,
                info != null ? info.getAvatarUrl() : null,
                storyDtos
        );
    }

    public List<UserStoryResponse> getFeed(Long viewerId) {

        List<Story> stories = storyRepository.findAllByIsDeletedFalse();

        // Lọc story theo quyền xem và hạn sử dụng
        List<Story> filtered = stories.stream()
                .filter(s -> !isExpired(s))
                .filter(s -> !s.getUserId().equals(viewerId))
                .filter(s -> storySettingService.canViewStory(viewerId, s.getUserId()))
                .toList();


        return buildUserStoryResponse(filtered, viewerId);
    }

    private boolean isExpired(Story s) {
        return s.getExpiredAt().isBefore(LocalDateTime.now());
    }

    private List<UserStoryResponse> buildUserStoryResponse(List<Story> stories, Long viewerId) {

        if (stories == null || stories.isEmpty())
            return List.of();

        // Group story theo userId
        Map<Long, List<Story>> grouped = stories.stream()
                .collect(Collectors.groupingBy(Story::getUserId));

        // Lấy danh sách userId
        List<Long> userIds = grouped.keySet().stream().toList();

        // Lấy thông tin user từ IAM
        List<UserInfo> userInfos = iamClient.getUserInfoBatch(userIds).getData();

        Map<Long, UserInfo> infoMap = userInfos.stream()
                .collect(Collectors.toMap(UserInfo::getId, u -> u));

        // Map sang UserStoryResponse
        return grouped.entrySet().stream()
                .map(entry -> {

                    Long userId = entry.getKey();
                    List<Story> userStories = entry.getValue();

                    UserInfo info = infoMap.get(userId);

                    List<StoryResponse> storyDtos = userStories.stream()
                            .map(s -> {
                                StoryResponse dto = storyMapper.toStoryResponse(s);
                                dto.setViewed(storyViewRepository.existsByStoryIdAndUserId(
                                        s.getId(),
                                        viewerId
                                ));
                                return dto;
                            })
                            .toList();

                    return new UserStoryResponse(
                            userId,
                            info != null ? info.getUsername() : null,
                            info != null ? info.getAvatarUrl() : null,
                            storyDtos
                    );
                })
                .toList();
    }
}
