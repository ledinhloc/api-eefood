package com.eefood.reactionservice.controller;

import com.eefood.reactionservice.dto.response.ApprovePostResponse;
import com.eefood.reactionservice.dto.response.ResponseData;
import com.eefood.reactionservice.model.ApprovePost;
import com.eefood.reactionservice.service.post.ApprovePostService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/posts")
@RequiredArgsConstructor
public class ApprovePostController {

  private final ApprovePostService service;

//  @PostMapping("/{postId}/approve")
//  public ResponseEntity<ApprovePostResponse> create(@RequestBody ApprovePost approvePost) {
//    ApprovePost saved = service.save(approvePost);
//    return ResponseEntity.ok(saved);
//  }

  @GetMapping("/{postId}/approve-history")
  public ResponseData<List<ApprovePostResponse>> getByPostId(@PathVariable Long postId) {
    List<ApprovePostResponse> results = service.getByPostId(postId);
    return new ResponseData<>(HttpStatus.OK.value(), HttpStatus.OK.getReasonPhrase(), results);
  }
}
