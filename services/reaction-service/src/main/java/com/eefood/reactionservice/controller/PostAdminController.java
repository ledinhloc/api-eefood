package com.eefood.reactionservice.controller;

import com.eefood.reactionservice.dto.response.PostResponse;
import com.eefood.reactionservice.dto.response.ResponseData;
import com.eefood.reactionservice.service.post.PostService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class PostAdminController {
    private final PostService postService;

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/posts")
    public ResponseData<Page<PostResponse>> getAllPostsByAdmin(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String region,
            @RequestParam(required = false) String difficulty,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Integer minPrepTime,
            @RequestParam(required = false) Integer maxPrepTime,
            @RequestParam(required = false) Integer minCookTime,
            @RequestParam(required = false) Integer maxCookTime,
            @RequestParam(required = false) Integer minReactionCount,
            @RequestParam(required = false) Integer minTotalShares,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "newest") String sortBy,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page - 1, size);

        Page<PostResponse> result = postService.getAllPostsByAdmin(
                keyword, userId, region, difficulty, category,
                minPrepTime, maxPrepTime, minCookTime, maxCookTime,
                minReactionCount, minTotalShares,
                status, sortBy, pageable
        );

        return new ResponseData<>(
                HttpStatus.OK.value(),
                "Success",
                result
        );
    }
}
