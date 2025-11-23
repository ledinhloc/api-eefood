package com.eefood.reactionservice.controller;

import com.eefood.reactionservice.dto.request.StoryCommentRequest;
import com.eefood.reactionservice.dto.response.ResponseData;
import com.eefood.reactionservice.dto.response.StoryCommentResponse;
import com.eefood.reactionservice.service.StoryCommentService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/story/comments")
@RequiredArgsConstructor
public class StoryCommentController {
    private final StoryCommentService storyCommentService;

    @PostMapping
    public ResponseData<StoryCommentResponse> addComment(@RequestBody StoryCommentRequest storyCommentRequest) {
        var result = storyCommentService.addComment(storyCommentRequest);
        return new ResponseData<>(
                HttpStatus.OK.value(),
                "Created success",
                result
        );
    }

    @PutMapping
    public ResponseData<StoryCommentResponse> updateComment(@RequestBody StoryCommentRequest storyCommentRequest) {
        var result = storyCommentService.updateComment(storyCommentRequest);
        return new ResponseData<>(
                HttpStatus.OK.value(),
                "Created success",
                result
        );
    }

    @DeleteMapping("/{commentId}")
    public ResponseData<Void> deleteComment(@PathVariable Long commentId) {
        storyCommentService.deleteComment(commentId);
        return new ResponseData<>(
                HttpStatus.OK.value(),
                "Deleted success"
        );
    }

    @GetMapping("/{storyId}")
    public ResponseData<Page<StoryCommentResponse>> getComments(
            @PathVariable Long storyId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") Sort.Direction sortDirection)
    {
        Pageable pageable = PageRequest.of(page - 1, limit, Sort.by(sortDirection, sortBy));
        Page<StoryCommentResponse> response = storyCommentService.getComments(storyId, pageable);
        return new ResponseData<>(
                HttpStatus.OK.value(),
                "Success",
                response
        );
    }

    @GetMapping("/replies/{parentId}")
    public ResponseData<Page<StoryCommentResponse>> getReplyComments(
            @PathVariable Long parentId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "ASC") Sort.Direction sortDirection)
    {
        Pageable pageable = PageRequest.of(page - 1, limit, Sort.by(sortDirection, sortBy));
        Page<StoryCommentResponse> response = storyCommentService.getReplyComments(parentId, pageable);
        return new ResponseData<>(
                HttpStatus.OK.value(),
                "Success",
                response
        );
    }
}
