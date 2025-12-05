package com.eefood.reactionservice.controller;

import com.eefood.reactionservice.dto.request.StoryCollectionRequest;
import com.eefood.reactionservice.dto.response.ResponseData;
import com.eefood.reactionservice.dto.response.StoryCollectionResponse;
import com.eefood.reactionservice.dto.response.StoryResponse;
import com.eefood.reactionservice.service.story.StoryCollectionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/story-collection")
@RequiredArgsConstructor
public class StoryCollectionController {
    private final StoryCollectionService storyCollectionService;

    @PostMapping
    public ResponseData<StoryCollectionResponse> createStoryCollection(@RequestBody StoryCollectionRequest request) {
        StoryCollectionResponse response = storyCollectionService.create(request);
        return new ResponseData<>(HttpStatus.CREATED.value(), "Story collection created successfully", response);
    }

    @PutMapping("/{id}")
    public ResponseData<StoryCollectionResponse> updateStoryCollection(@PathVariable Long id, @RequestBody StoryCollectionRequest request) {
        StoryCollectionResponse response = storyCollectionService.update(id, request);
        return new ResponseData<>(HttpStatus.OK.value(), "Story collection updated successfully", response);
    }

    @DeleteMapping("/{id}")
    public ResponseData<Void> deleteStoryCollection(@PathVariable Long id) {
        storyCollectionService.delete(id);
        return new ResponseData<>(HttpStatus.OK.value(), "Story collection deleted successfully");
    }

    @GetMapping
    public ResponseData<Page<StoryCollectionResponse>> getUserCollections(
            @RequestParam Long userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") Sort.Direction direction
    ) {
        Pageable pageable = PageRequest.of(page - 1, limit, Sort.by(direction, sortBy));
        Page<StoryCollectionResponse> responses = storyCollectionService.getUserCollections(userId, pageable);
        return new ResponseData<>(HttpStatus.OK.value(), "Successfully", responses);
    }

    @GetMapping("/{id}/user/{userId}")
    public ResponseData<List<StoryResponse>> getAllStoryCollections(@PathVariable Long id, @PathVariable Long userId) {
        List<StoryResponse> storyResponses = storyCollectionService.getStoriesInCollection(id, userId);
        return new ResponseData<>(HttpStatus.OK.value(), "Successfully", storyResponses);
    }

    @PostMapping("/{collectionId}/story/{storyId}")
    public ResponseData<Void> addStoryToCollection(@PathVariable Long collectionId, @PathVariable Long storyId)
    {
        storyCollectionService.addStoryToCollection(collectionId, storyId);
        return new ResponseData<>(HttpStatus.OK.value(), "Added story to collection successfully");
    }

    @DeleteMapping("/{collectionId}/story/{storyId}")
    public ResponseData<Void> removeStoryToCollection(@PathVariable Long collectionId, @PathVariable Long storyId)
    {
        storyCollectionService.removeStoryFromCollection(collectionId, storyId);
        return new ResponseData<>(HttpStatus.OK.value(), "Removed story to collection successfully");
    }

    @GetMapping("/containing/{storyId}")
    public ResponseData<List<StoryCollectionResponse>> getCollectionsContainingStory(@PathVariable Long storyId) {
        List<StoryCollectionResponse> responses = storyCollectionService.getCollectionsContainingStory(storyId);
        return new ResponseData<>(HttpStatus.OK.value(), "Successfully", responses);
    }
}
