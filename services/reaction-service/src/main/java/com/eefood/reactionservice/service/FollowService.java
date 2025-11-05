package com.eefood.reactionservice.service;

import com.eefood.reactionservice.dto.response.ResponseData;
import com.eefood.reactionservice.dto.response.UserInfo;
import com.eefood.reactionservice.model.Follow;
import com.eefood.reactionservice.repository.FollowRepository;
import com.eefood.reactionservice.repository.httpclient.IamClient;
import com.eefood.reactionservice.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FollowService {
    private final FollowRepository followRepository;
    private final SecurityUtil securityUtil;
    private final IamClient iamClient;

    @Transactional
    public boolean toggleFollow(Long followingId) {
        Long currentUserId = securityUtil.getCurrentUserId();

        if(currentUserId.equals(followingId)) {
            throw new RuntimeException("You cannot follow yourself.");
        }

        var existing = followRepository.findByFollowerIdAndFollowingId(currentUserId, followingId);

        if(existing.isPresent()) {
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
            return true;
        }
    }

    public boolean checkFollow(Long followingId) {
        Long currentUserId = securityUtil.getCurrentUserId();
        return followRepository.findByFollowerIdAndFollowingId(currentUserId, followingId).isPresent();
    }

    public List<UserInfo> getFollowers(Long userId) {
        List<Long> followerIds = followRepository.findByFollowingId(userId)
                .stream()
                .map(Follow::getFollowerId)
                .collect(Collectors.toList());

        if(followerIds.isEmpty()) return List.of();

        ResponseData<List<UserInfo>> response = iamClient.getUserInfoBatch(followerIds);
        return response.getData();
    }

    public List<UserInfo> getFollowing(Long userId) {
        List<Long> followingIds = followRepository.findByFollowingId(userId)
                .stream()
                .map(Follow::getFollowingId)
                .collect(Collectors.toList());

        if(followingIds.isEmpty()) return List.of();

        ResponseData<List<UserInfo>> response = iamClient.getUserInfoBatch(followingIds);
        return response.getData();
    }

    public Map<String, Long> getFollowStats(Long userId) {
        long followers = followRepository.countByFollowingId(userId);
        long followings = followRepository.countByFollowerId(userId);
        return Map.of("followers", followers, "followings", followings);
    }
}
