package com.eefood.reactionservice.controller;

import com.eefood.reactionservice.dto.request.CommentReactionRequest;
import com.eefood.reactionservice.dto.response.CommentReactionResponse;
import com.eefood.reactionservice.dto.response.ResponseData;
import com.eefood.reactionservice.enums.ReactionType;
import com.eefood.reactionservice.service.CommentReactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/comment-reactions")
@RequiredArgsConstructor
public class ConmmentReactionController {
    private final CommentReactionService commentReactionService;

    @PostMapping("")
    public ResponseData<CommentReactionResponse> reactToComment(@RequestBody CommentReactionRequest commentReactionRequest) {
        CommentReactionResponse response = commentReactionService.reactToComment(commentReactionRequest);
        return new ResponseData<>(HttpStatus.OK.value(),"Successfully reacted to comment",response);
    }

    @DeleteMapping("/{commentId}")
    public ResponseData<Void> removeReaction(@PathVariable Long commentId) {
        commentReactionService.removeReaction(commentId);
        return new ResponseData<>(HttpStatus.OK.value(),"Successfully reacted to comment");
    }

    @GetMapping("/{commentId}")
    public ResponseData<List<CommentReactionResponse>> getReaction(@PathVariable Long commentId) {
        return new ResponseData<>(HttpStatus.OK.value(), "Get Success", commentReactionService.getUserReactionsByComment(commentId));
    }

    @GetMapping("/{commentId}/list")
    public ResponseData<Page<CommentReactionResponse>> getReaction(
            @PathVariable Long commentId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") Sort.Direction sortDirection) {
        Pageable pageable = PageRequest.of(page-1, limit, Sort.by(sortDirection, sortBy));
        return new ResponseData<>(
                HttpStatus.OK.value(),
                "Get Success",
                commentReactionService.getReactionsByComment(commentId, pageable)
        );
    }

    @GetMapping("/{commentId}/counts")
    public ResponseData<Map<ReactionType, Long>> getReactionCounts(@PathVariable Long commentId) {
        return new ResponseData<>(HttpStatus.OK.value(),
                "Get reaction counts success",
                commentReactionService.getReactionCounts(commentId));
    }
}
