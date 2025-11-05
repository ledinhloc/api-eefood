package com.eefood.reactionservice.controller;

import com.eefood.reactionservice.dto.response.ResponseData;
import com.eefood.reactionservice.dto.response.UserInfo;
import com.eefood.reactionservice.service.FollowService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/follows")
@RequiredArgsConstructor
public class FollowController {
    private final FollowService followService;

    @PostMapping("{targetId}")
    public ResponseData<Boolean> toggleFollow(@PathVariable Long targetId) {
        boolean isAllowed = followService.toggleFollow(targetId);
        return new ResponseData<>(HttpStatus.OK.value(), "Success", isAllowed);
    }

    @GetMapping("check/{targetId}")
    public ResponseData<Boolean> checkFollow(@PathVariable Long targetId) {
        boolean isAllowed = followService.checkFollow(targetId);
        return new ResponseData<>(HttpStatus.OK.value(), "Success", isAllowed);
    }

    @GetMapping("/followers/{userId}")
    public ResponseData<List<UserInfo>> getFollowers(@PathVariable Long userId) {
        List<UserInfo> followers = followService.getFollowers(userId);
        return new ResponseData<>(HttpStatus.OK.value(), "Success", followers);
    }

    @GetMapping("/following/{userId}")
    public ResponseData<List<UserInfo>> getFollowings(@PathVariable Long userId) {
        List<UserInfo> followings = followService.getFollowing(userId);
        return new ResponseData<>(HttpStatus.OK.value(), "Success", followings);
    }

    @GetMapping("/stats/{userId}")
    public ResponseData<Map<String, Long>> getStats(@PathVariable Long userId) {
        Map<String, Long> counts = followService.getFollowStats(userId);
        return new ResponseData<>(HttpStatus.OK.value(), "Success", counts);
    }
}
