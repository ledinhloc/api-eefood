package com.eefood.reactionservice.controller;

import com.eefood.reactionservice.dto.request.StoryRequest;
import com.eefood.reactionservice.dto.response.ResponseData;
import com.eefood.reactionservice.dto.response.StoryResponse;
import com.eefood.reactionservice.service.StoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/story")
@RequiredArgsConstructor
public class StoryController {
    private final StoryService storyService;

    @PostMapping
    public ResponseData<StoryResponse> createStory(@RequestBody StoryRequest storyRequest) {
        var result = storyService.createStory(storyRequest);
        return new ResponseData<>(
                HttpStatus.OK.value(),
                "Created success",
                result
        );
    }

    @PutMapping
    public ResponseData<StoryResponse> updateStory(@RequestBody StoryRequest storyRequest) {
        var result = storyService.updateStory(storyRequest);
        return new ResponseData<>(
                HttpStatus.OK.value(),
                "Updated success",
                result
        );
    }

    @DeleteMapping("/{id}")
    public ResponseData<StoryResponse> deleteStory(@PathVariable Long id) {
        storyService.softDeleteStory(id);
        return new ResponseData<>(
                HttpStatus.OK.value(),
                "Deleted success"
        );
    }
}
