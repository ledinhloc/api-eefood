package com.eefood.reactionservice.controller;

import com.eefood.reactionservice.dto.request.StoryReactionRequest;
import com.eefood.reactionservice.dto.response.ResponseData;
import com.eefood.reactionservice.dto.response.StoryReactionResponse;
import com.eefood.reactionservice.service.story.StoryReactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/story-reactions")
@RequiredArgsConstructor
public class StoryReactionController {
    private final StoryReactionService storyReactionService;

    @GetMapping("/{storyId}/current")
    public ResponseData<StoryReactionResponse> getCurrentUserReaction(
            @PathVariable Long storyId
    ) {
        var response = storyReactionService.getUserReactionForStory(storyId);
        return new ResponseData<>(HttpStatus.OK.value(),"Successfully reacted to story",response);
    }

    @PostMapping
    public ResponseData<StoryReactionResponse> reactToStory(@RequestBody StoryReactionRequest request) {
        StoryReactionResponse response = storyReactionService.reactToStory(request);
        return new ResponseData<>(HttpStatus.OK.value(),"Successfully reacted to story",response);
    }

    @DeleteMapping("/{storyId}")
    public ResponseData<Void> removeReaction(@PathVariable Long storyId) {
        storyReactionService.removeReaction(storyId);
        return new ResponseData<>(HttpStatus.OK.value(),"Successfully deleted react to story");
    }

    @GetMapping("/{storyId}/users")
    public ResponseData<Page<StoryReactionResponse>> getUsersReactedStory(
            @PathVariable Long storyId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") Sort.Direction sortDirection
    ) {
        Pageable pageable = PageRequest.of(page-1, limit, Sort.by(sortDirection, sortBy));
        Page<StoryReactionResponse> response = storyReactionService.getUsersReactedStory(storyId, pageable);
        return new ResponseData<>(HttpStatus.OK.value(), "Fetched users reacted successfully", response);
    }

    @GetMapping("/{storyId}/total")
    public ResponseData<Long> getTotalReactions(@PathVariable Long storyId) {
        Long total = storyReactionService.getTotalReactions(storyId);
        return new ResponseData<>(HttpStatus.OK.value(), "Fetched total reactions successfully", total);
    }
}
