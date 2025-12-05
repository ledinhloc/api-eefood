package com.eefood.reactionservice.controller;

import com.eefood.reactionservice.dto.request.StoryRequest;
import com.eefood.reactionservice.dto.request.StoryViewRequest;
import com.eefood.reactionservice.dto.response.ResponseData;
import com.eefood.reactionservice.dto.response.StoryResponse;
import com.eefood.reactionservice.dto.response.StoryViewResponse;
import com.eefood.reactionservice.dto.response.UserStoryResponse;
import com.eefood.reactionservice.service.story.StoryService;
import com.eefood.reactionservice.service.story.StoryViewService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/story")
@RequiredArgsConstructor
public class StoryController {
    private final StoryService storyService;
    private final StoryViewService storyViewService;

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
    public ResponseData<Void> deleteStory(@PathVariable Long id) {
        storyService.softDeleteStory(id);
        return new ResponseData<>(
                HttpStatus.OK.value(),
                "Deleted success"
        );
    }

    @GetMapping("/me")
    public ResponseData<UserStoryResponse> getOwnStory() {
        var result = storyService.getOwnStory();
        return new ResponseData<>(
                HttpStatus.OK.value(),
                "Success",
                result
        );
    }

    @GetMapping("/{viewerId}")
    public ResponseData<List<UserStoryResponse>> getFeed(@PathVariable Long viewerId) {
        var result = storyService.getFeed(viewerId);
        return new ResponseData<>(
                HttpStatus.OK.value(),
                "Success",
                result
        );
    }

    @PostMapping("/view")
    public ResponseData<Void> viewStory(@RequestBody StoryViewRequest request) {
        storyViewService.viewStory(request.getStoryId(), request.getViewerId());
        return new ResponseData<>(
                HttpStatus.OK.value(),
                "Success"
        );
    }

    @GetMapping("/get-viewer")
    public ResponseData<Page<StoryViewResponse>> getViewer(
                                        @RequestParam Long storyId,
                                        @RequestParam(defaultValue = "1") int page,
                                        @RequestParam(defaultValue = "5") int limit)
    {
        Pageable pageable = PageRequest.of(page-1, limit);
        var result = storyViewService.getStoryViews(storyId, pageable);
        return new ResponseData<>(
                HttpStatus.OK.value(),
                "Success",result
        );
    }
}
