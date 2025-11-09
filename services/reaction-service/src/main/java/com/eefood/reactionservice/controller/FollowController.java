package com.eefood.reactionservice.controller;

import com.eefood.reactionservice.dto.response.FollowResponse;
import com.eefood.reactionservice.dto.response.ResponseData;
import com.eefood.reactionservice.dto.response.UserInfo;
import com.eefood.reactionservice.service.FollowService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/follows")
@RequiredArgsConstructor
public class FollowController {
    private final FollowService followService;

    @PostMapping("/{targetId}")
    public ResponseData<Boolean> toggleFollow(@PathVariable Long targetId) {
        boolean isAllowed = followService.toggleFollow(targetId);
        return new ResponseData<>(HttpStatus.OK.value(), "Success", isAllowed);
    }

    @DeleteMapping("/{targetId}")
    public ResponseData<Boolean> unFollow(@PathVariable Long targetId) {
        boolean isAllowed = followService.unFollow(targetId);
        return new ResponseData<>(HttpStatus.OK.value(), "Success", isAllowed);
    }

    @GetMapping("/check/{targetId}")
    public ResponseData<Boolean> checkFollow(@PathVariable Long targetId) {
        boolean isAllowed = followService.checkFollow(targetId);
        return new ResponseData<>(HttpStatus.OK.value(), "Success", isAllowed);
    }

    @GetMapping("/followers/{userId}")
    public ResponseData<Page<FollowResponse>> getFollowers(@PathVariable Long userId,
                                                     @RequestParam(defaultValue = "1") int page,
                                                     @RequestParam(defaultValue = "10") int limit) {
        Pageable pageable = PageRequest.of(page-1, limit);
        Page<FollowResponse> followers = followService.getFollowers(userId,pageable);
        return new ResponseData<>(HttpStatus.OK.value(), "Success", followers);
    }

    @GetMapping("/followings/{userId}")
    public ResponseData<Page<FollowResponse>> getFollowings(@PathVariable Long userId,
                                                      @RequestParam(defaultValue = "1") int page,
                                                      @RequestParam(defaultValue = "10") int limit) {
        Pageable pageable = PageRequest.of(page-1, limit);
        Page<FollowResponse> followings = followService.getFollowing(userId, pageable);
        return new ResponseData<>(HttpStatus.OK.value(), "Success", followings);
    }

    @GetMapping("/stats/{userId}")
    public ResponseData<Map<String, Long>> getStats(@PathVariable Long userId) {
        Map<String, Long> counts = followService.getFollowStats(userId);
        return new ResponseData<>(HttpStatus.OK.value(), "Success", counts);
    }
}
