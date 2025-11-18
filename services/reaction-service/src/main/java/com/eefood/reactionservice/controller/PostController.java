package com.eefood.reactionservice.controller;
import com.eefood.reactionservice.dto.request.PostCreateRequest;
import com.eefood.reactionservice.dto.response.PostPublishResponse;
import com.eefood.reactionservice.dto.response.PostResponse;
import com.eefood.reactionservice.dto.response.ResponseData;
import com.eefood.reactionservice.service.GeminiService;
import com.eefood.reactionservice.service.PostService;
import com.eefood.reactionservice.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/v1/posts")
@RequiredArgsConstructor
@Slf4j
public class PostController {
  private final PostService postService;
  private final GeminiService geminiService;
  private final SecurityUtil securityUtil;

  @PostMapping("/import")
  public ResponseData<PostPublishResponse> importPost(@RequestBody PostCreateRequest request, @RequestParam Long userId) {
    PostPublishResponse post = postService.createPost(request, userId);
    log.info("-----import success postId: " + post.getId()+ ", recipeId"+ post.getRecipeId() + " " + post.getTitle());
    return new ResponseData<>(HttpStatus.CREATED.value(), "Post created successfully", post);
  }

  @PostMapping
  public ResponseData<PostPublishResponse> createPost(@RequestBody PostCreateRequest request) {
    //Dùng userId từ header
    Long postUserId = securityUtil.getCurrentUserId();
    PostPublishResponse post = postService.createPost(request, postUserId);
    log.info("-----user insert success postId: " + post.getId()+ ", recipeId"+ post.getRecipeId() + " " + post.getTitle());
    return new ResponseData<>(HttpStatus.CREATED.value(), "Post created successfully", post);
  }

  @PostMapping(value = "/get-keyword", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseData<String> getKeyword(@RequestParam MultipartFile image) {
    String result = geminiService.extractKeywordsFromImage(image);
    return new ResponseData<>(
            HttpStatus.OK.value(),
            "Success",
            result
    );
  }

  @GetMapping("/user")
  public ResponseData<List<PostPublishResponse>> getPostsPublishByUser() {
    List<PostPublishResponse> result = postService.getPostsPublishByUser();
    return new ResponseData<>(HttpStatus.OK.value(), "Success", result);
  }

  @PutMapping("/{id}")
  public ResponseData<PostPublishResponse> updatePost(@PathVariable Long id,  @RequestBody Map<String, String> requestBody) {
    String content = requestBody.get("content");
    PostPublishResponse post = postService.updatePost(id, content);
    return new ResponseData<>(HttpStatus.OK.value(), "Post updated successfully", post);
  }

  @DeleteMapping("/{id}")
  public ResponseData<Void> deletePost(@PathVariable Long id) {
    postService.deletePost(id);
    return new ResponseData<>(HttpStatus.OK.value(), "Post deleted successfully");
  }

  @GetMapping
  public ResponseData<Page<PostResponse>> getAllPosts(
    @RequestParam(required = false) String keyword,
    @RequestParam(required = false) Long userId,
    @RequestParam(required = false) String region,
    @RequestParam(required = false) String difficulty,
    @RequestParam(required = false) String category,
    @RequestParam(required = false) Integer maxCookTime,
    @RequestParam(defaultValue = "1") int page,
    @RequestParam(defaultValue = "10") int size
  ) {
    Page<PostResponse> result = postService.getAllPosts(
      keyword,
      userId,
      region,
      difficulty,
      category,
      maxCookTime,
      page,
      size
    );

    return new ResponseData<>(
      HttpStatus.OK.value(),
      "Success",
      result
    );
  }

  @GetMapping("/{id}")
  public ResponseData<PostResponse> getPostById(@PathVariable Long id) {
    PostResponse post = postService.getPostById(id);
    return new ResponseData<>(HttpStatus.OK.value(), "Success", post);
  }
}
