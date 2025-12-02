package com.eefood.reactionservice.controller;

import com.eefood.reactionservice.dto.request.ShareRequest;
import com.eefood.reactionservice.dto.response.ResponseData;
import com.eefood.reactionservice.dto.response.ShareResponse;
import com.eefood.reactionservice.service.share.ShareService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/shares")
@RequiredArgsConstructor
public class ShareController {
    private final ShareService shareService;

    @PostMapping
    public ResponseData<ShareResponse> sharePost(@RequestBody ShareRequest shareRequest) {
        ShareResponse shareResponse = shareService.sharePost(shareRequest);
        return new ResponseData<>(HttpStatus.OK.value(), "Share successfully", shareResponse);
    }

    @GetMapping("/count/{postId}")
    public ResponseData<Long> countPost(@PathVariable Long postId) {
        Long count  = shareService.getShareCountByPostId(postId);
        return new ResponseData<>(HttpStatus.OK.value(), "Success", count);
    }
}
