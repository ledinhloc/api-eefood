package com.eefood.reactionservice.controller;

import com.eefood.reactionservice.dto.request.CommentRequest;
import com.eefood.reactionservice.dto.response.CommentResponse;
import com.eefood.reactionservice.dto.response.ResponseData;
import com.eefood.reactionservice.service.comment.CommentService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/comments")
@RequiredArgsConstructor
public class CommentController {
    private final CommentService commentService;

    @GetMapping("/{commentId}")
    public ResponseData<CommentResponse> getCommentById(@PathVariable Long commentId) {
        return new ResponseData<>(HttpStatus.OK.value(), "Success", commentService.getCommentById(commentId));
    }

    @PutMapping("/{commentId}")
    public ResponseData<CommentResponse> updateComment(
            @PathVariable Long commentId,
            @RequestBody Map<String, String> body) {

        String newContent = body.get("content");
        CommentResponse updated = commentService.updateCommentContent(commentId, newContent);
        return new ResponseData<>(HttpStatus.OK.value(), "Comment updated successfully", updated);
    }

    @DeleteMapping("/{commentId}")
    public ResponseData<Void> deleteComment(@PathVariable Long commentId) {
        commentService.softDeleteComment(commentId);
        return new ResponseData<>(HttpStatus.OK.value(), "Comment deleted successfully");
    }

    @PostMapping("")
    public ResponseData<CommentResponse> createComment(@RequestBody CommentRequest commentRequest) {
        var result = commentService.addComment(commentRequest);
        return new ResponseData<>(HttpStatus.OK.value(), "Success", result);
    }

    @GetMapping("/post/{postId}")
    public ResponseData<Page<CommentResponse>> getRootComments(
    @PathVariable Long postId,
    @RequestParam(defaultValue = "1") int page,
    @RequestParam(defaultValue = "10") int limit,
    @RequestParam(defaultValue = "createdAt") String sortBy,
    @RequestParam(defaultValue = "DESC") Sort.Direction sortDirection
    ) {
        Pageable pageable = PageRequest.of(page-1, limit, Sort.by(sortDirection, sortBy));
        Page<CommentResponse> result = commentService.getPostComments(postId, pageable);
        return new ResponseData<>(HttpStatus.OK.value(), "Success", result);
    }

    @GetMapping("/{commentId}/replies")
    public ResponseData<Page<CommentResponse>> getRepliesComments(
            @PathVariable Long commentId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") Sort.Direction sortDirection
    ) {
        Pageable pageable = PageRequest.of(page-1, limit, Sort.by(sortDirection, sortBy));
        Page<CommentResponse> result = commentService.getCommentReplies(commentId, pageable);
        return new ResponseData<>(HttpStatus.OK.value(), "Success", result);
    }
}
