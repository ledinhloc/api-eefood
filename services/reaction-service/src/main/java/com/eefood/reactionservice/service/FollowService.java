package com.eefood.reactionservice.service;

import com.eefood.reactionservice.dto.response.FollowResponse;
import com.eefood.reactionservice.dto.response.ResponseData;
import com.eefood.reactionservice.dto.response.UserInfo;
import com.eefood.reactionservice.mapper.FollowMapper;
import com.eefood.reactionservice.model.Follow;
import com.eefood.reactionservice.model.StorySetting;
import com.eefood.reactionservice.repository.FollowRepository;
import com.eefood.reactionservice.repository.StorySettingRepository;
import com.eefood.reactionservice.repository.httpclient.IamClient;
import com.eefood.reactionservice.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FollowService {
    private final FollowRepository followRepository;
    private final SecurityUtil securityUtil;
    private final IamClient iamClient;
    private final FollowMapper followMapper;
    private final StorySettingRepository storySettingRepository;

    private void addToAllowedList(Long followerId, Long ownerId) {
        StorySetting setting = storySettingRepository.findByUserId(ownerId).orElse(null);
        if (setting == null) return;

        if (setting.getAllowedUserIds() == null) {
            setting.setAllowedUserIds(new ArrayList<>());
        }

        if (!setting.getAllowedUserIds().contains(followerId)) {
            setting.getAllowedUserIds().add(followerId);
            storySettingRepository.save(setting);
        }
    }

    private void removeFromAllowedList(Long followerId, Long ownerId) {
        StorySetting setting = storySettingRepository.findByUserId(ownerId).orElse(null);
        if (setting == null || setting.getAllowedUserIds() == null) return;

        if (setting.getAllowedUserIds().contains(followerId)) {
            setting.getAllowedUserIds().remove(followerId);
            storySettingRepository.save(setting);
        }
    }

    public List<Long> getNewFollowings(Long userId) {
    List<Follow> follows = followRepository.findByFollowerId(userId);

    return follows.stream()
      .filter(f -> f.getCreatedAt().isAfter(LocalDateTime.now().minusDays(3)))
      .map(Follow::getFollowingId)
      .toList();
    }

    public List<Long> getOldFollowings(Long userId) {
    List<Follow> follows = followRepository.findByFollowerId(userId);

    return follows.stream()
      .filter(f -> f.getCreatedAt().isBefore(LocalDateTime.now().minusDays(3)))
      .map(Follow::getFollowingId)
      .toList();
    }

    public List<Long> getFollowingIds(Long userId) {
    return followRepository.findByFollowerId(userId).stream()
      .map(Follow::getFollowingId)
      .toList();
    }

    @Transactional
    public boolean toggleFollow(Long followingId) {
        Long currentUserId = securityUtil.getCurrentUserId();

        if(currentUserId.equals(followingId)) {
            throw new RuntimeException("You cannot follow yourself.");
        }

        var existing = followRepository.findByFollowerIdAndFollowingId(currentUserId, followingId);

        if(existing.isPresent()) {
            // Khi unfollow thì xóa trong allowedUserIds
            removeFromAllowedList(currentUserId, followingId);
            followRepository.delete(existing.get());
            return false;
        }
        else {
            Follow follow = Follow.builder()
                    .followerId(currentUserId)
                    .followingId(followingId)
                    .createdAt(LocalDateTime.now())
                    .build();
            followRepository.save(follow);
            // Khi follow thì thêm vào allowedUserIds
            addToAllowedList(currentUserId, followingId);
            return true;
        }
    }

    @Transactional
    public boolean unFollow(Long followingId) {
        Long currentUserId = securityUtil.getCurrentUserId();

        if(currentUserId.equals(followingId)) {
            throw new RuntimeException("You cannot unfollow yourself.");
        }

        var existing = followRepository.findByFollowerIdAndFollowingId(currentUserId, followingId);

        if(existing.isPresent()) {
            followRepository.delete(existing.get());
            removeFromAllowedList(currentUserId, followingId);
            return true;
        }
        return false;
    }

    public boolean checkFollow(Long followingId) {
        Long currentUserId = securityUtil.getCurrentUserId();
        return followRepository.findByFollowerIdAndFollowingId(currentUserId, followingId).isPresent();
    }

    public Page<FollowResponse> getFollowers(Long userId, Pageable pageable) {
        Page<Follow> followPage = followRepository.findByFollowingId(userId, pageable);
        if (followPage.isEmpty()) return Page.empty(pageable);

        // Lấy danh sách người theo dõi (followerId)
        List<Long> followerIds = followPage.getContent()
                .stream()
                .map(Follow::getFollowerId)
                .distinct()
                .toList();

        return buildFollowResponsePage(followPage, followerIds, true, pageable);
    }

    public Page<FollowResponse> getFollowing(Long userId, Pageable pageable) {
        Page<Follow> followPage = followRepository.findByFollowerId(userId, pageable);
        if (followPage.isEmpty()) return Page.empty(pageable);

        // Lấy danh sách người mà user này theo dõi (followingId)
        List<Long> followingIds = followPage.getContent()
                .stream()
                .map(Follow::getFollowingId)
                .distinct()
                .toList();

        return buildFollowResponsePage(followPage, followingIds, false, pageable);
    }

    public Map<String, Long> getFollowStats(Long userId) {
        long followers = followRepository.countByFollowingId(userId);
        long followings = followRepository.countByFollowerId(userId);
        return Map.of("followers", followers, "followings", followings);
    }

    private Page<FollowResponse> buildFollowResponsePage(
            Page<Follow> followPage,
            List<Long> userIds,
            boolean isFollowers,
            Pageable pageable
    ) {
        ResponseData<List<UserInfo>> response = iamClient.getUserInfoBatch(userIds);
        List<UserInfo> userInfos = Optional.ofNullable(response.getData()).orElse(List.of());

        if (userInfos.isEmpty()) {
            return new PageImpl<>(List.of(), pageable, followPage.getTotalElements());
        }

        Map<Long, UserInfo> userInfoMap = userInfos.stream()
                .collect(Collectors.toMap(UserInfo::getId, u -> u));

        List<FollowResponse> responses = followPage.getContent().stream().map(f -> {
            FollowResponse dto = followMapper.toResponse(f);
            Long lookupId = isFollowers ? f.getFollowerId() : f.getFollowingId();
            dto.setFollow(checkFollow(lookupId));
            UserInfo u = userInfoMap.get(lookupId);

            if (u != null) {
                dto.setUsername(u.getUsername());
                dto.setEmail(u.getEmail());
                dto.setAvatarUrl(u.getAvatarUrl());
            }
            return dto;
        }).toList();

        return new PageImpl<>(responses, pageable, followPage.getTotalElements());
    }
}
