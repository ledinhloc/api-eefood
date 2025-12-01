package com.eefood.reactionservice.controller;

import com.eefood.reactionservice.dto.request.StorySettingRequest;
import com.eefood.reactionservice.dto.response.ResponseData;
import com.eefood.reactionservice.dto.response.StorySettingResponse;
import com.eefood.reactionservice.service.story.StorySettingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/story/settings")
@RequiredArgsConstructor
public class StorySettingController {
    private final StorySettingService storySettingService;

    @GetMapping("/{userId}")
    public ResponseData<StorySettingResponse> getSetting(@PathVariable Long userId) {
        var res = storySettingService.getSetting(userId);
        return new ResponseData<>(
                HttpStatus.OK.value(),
                "Success",
                res
        );
    }

    @PostMapping
    public ResponseData<StorySettingResponse> save(@RequestBody StorySettingRequest request) {
        var result = storySettingService.createOrUpdateSetting(request);
        return new ResponseData<>(
                HttpStatus.OK.value(),
                "Success",
                result
        );
    }

    @DeleteMapping("/{userId}")
    public ResponseData<Void> delete(@PathVariable Long userId) {
        storySettingService.deleteSetting(userId);
        return new ResponseData<>(
                HttpStatus.OK.value(),
                "Success"
        );
    }
}
