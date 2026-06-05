package com.eefood.reactionservice.livestream.controller;

import com.eefood.reactionservice.dto.response.ResponseData;
import com.eefood.reactionservice.livestream.dto.response.LeaderboardEntryResponse;
import com.eefood.reactionservice.livestream.dto.response.LivePollSettingResponse;
import com.eefood.reactionservice.livestream.service.LiveLeaderboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/livestreams")
@RequiredArgsConstructor
public class LiveLeaderboardController {
    private final LiveLeaderboardService leaderboardService;

    @GetMapping("/{livestreamId}/leaderboard")
    public ResponseData<List<LeaderboardEntryResponse>> getLeaderboard(@PathVariable Long livestreamId) {
        List<LeaderboardEntryResponse> responses = leaderboardService.getTop10(livestreamId);
        return new ResponseData<>(HttpStatus.OK.value(),"Success",responses);
    }
}
