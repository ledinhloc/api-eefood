package com.eefood.reactionservice.controller;


import com.eefood.reactionservice.dto.response.PostResponse;
import com.eefood.reactionservice.dto.response.ResponseData;
import com.eefood.reactionservice.service.PostService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/posts")
@RequiredArgsConstructor
public class PostController {
  private final PostService postService;

  @GetMapping
  public ResponseData<Page<PostResponse>> getAllPosts(
    @RequestParam(required = false) String keyword,
    @RequestParam(required = false) Long userId,
    @RequestParam(required = false) String region,
    @RequestParam(required = false) String difficulty,
    @RequestParam(defaultValue = "1") int page,
    @RequestParam(defaultValue = "10") int size,
    @RequestParam(defaultValue = "createdAt") String sortBy,
    @RequestParam(defaultValue = "DESC")Sort.Direction sortDirection
    ) {
    Pageable pageable = PageRequest.of(page-1, size, Sort.by(sortDirection, sortBy));
    Page<PostResponse> result = postService.getAllPosts(keyword, userId, region, difficulty, pageable);
    return new ResponseData<>(HttpStatus.OK.value(), "Success", result);
  }

  @GetMapping("/{id}")
  public ResponseData<PostResponse> getPostById(@PathVariable Long id) {
    PostResponse post = postService.getPostById(id);
    return new ResponseData<>(HttpStatus.OK.value(), "Success", post);
  }
}
