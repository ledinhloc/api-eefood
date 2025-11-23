package com.eefood.reactionservice.service;

import com.eefood.reactionservice.dto.response.StoryViewResponse;
import com.eefood.reactionservice.dto.response.UserInfo;
import com.eefood.reactionservice.mapper.StoryViewMapper;
import com.eefood.reactionservice.model.Story;
import com.eefood.reactionservice.model.StoryView;
import com.eefood.reactionservice.repository.StoryRepository;
import com.eefood.reactionservice.repository.StoryViewRepository;
import com.eefood.reactionservice.repository.httpclient.IamClient;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StoryViewService {
    private final StoryRepository storyRepository;
    private final StoryViewRepository storyViewRepository;
    private final StorySettingService storySettingService;
    private final StoryViewMapper storyViewMapper;
    private final IamClient iamClient;

    public void viewStory(Long storyId, Long viewerId) {
        Story story = storyRepository.findById(storyId).orElseThrow(() -> new RuntimeException("Story not found or deleted"));

        if (!storySettingService.canViewStory(viewerId, story.getUserId())) {
            throw new RuntimeException("Not allowed to view story");
        }

        if (!storyViewRepository.existsByStoryIdAndUserId(storyId, viewerId)) {
            StoryView view = StoryView.builder()
                    .story(story)
                    .userId(viewerId)
                    .build();
            storyViewRepository.save(view);
        }
    }

    public Page<StoryViewResponse> getStoryViews(Long storyId, Pageable pageable) {
        Page<StoryView> page = storyViewRepository.findAllByStoryId(storyId, pageable);

        if (page.isEmpty()) return Page.empty();

        // Lấy userIds
        List<Long> userIds = page.getContent().stream()
                .map(StoryView::getUserId)
                .distinct()
                .toList();

        // Lấy thông tin user từ IAM
        List<UserInfo> userInfos = iamClient.getUserInfoBatch(userIds).getData();
        Map<Long, UserInfo> userInfoMap = userInfos.stream()
                .collect(Collectors.toMap(UserInfo::getId, u -> u));

        // Map sang DTO
        List<StoryViewResponse> dtoList = page.getContent().stream()
                .map(view -> {
                    StoryViewResponse dto = storyViewMapper.toResponse(view);
                    UserInfo info = userInfoMap.get(view.getUserId());
                    if (info != null) {
                        dto.setUsername(info.getUsername());
                        dto.setEmail(info.getEmail());
                        dto.setAvatarUrl(info.getAvatarUrl());
                    }
                    return dto;
                })
                .toList();

        return new PageImpl<>(dtoList, pageable, page.getTotalElements());
    }
}
