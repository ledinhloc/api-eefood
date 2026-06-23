package com.eefood.reactionservice.controller;
import com.eefood.reactionservice.dto.request.PostCreateRequest;
import com.eefood.reactionservice.dto.response.PostPublishResponse;
import com.eefood.reactionservice.dto.response.PostResponse;
import com.eefood.reactionservice.dto.response.ResponseData;
import com.eefood.reactionservice.dto.response.SimilarPostResponse;
import com.eefood.reactionservice.service.ai.OpenAIImageService;
import com.eefood.reactionservice.service.post.PostService;
import com.eefood.reactionservice.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/posts")
@RequiredArgsConstructor
@Slf4j
public class PostController {
  private final PostService postService;
  private final OpenAIImageService openAIImageService;
  private final SecurityUtil securityUtil;

  @PostMapping("/import")
  public ResponseData<PostPublishResponse> importPost(@RequestBody PostCreateRequest request, @RequestParam Long userId) {
    PostPublishResponse post = postService.createPost(request, userId);
    log.info("-----import success postId: " + post.getId()+ ", recipeId"+ post.getRecipeId() + " " + post.getTitle());
    return new ResponseData<>(HttpStatus.CREATED.value(), "Post created successfully", post);
  }

  @PostMapping
  public ResponseData<PostPublishResponse> createPost(@RequestBody PostCreateRequest request) {
    Long postUserId = securityUtil.getCurrentUserId();
    PostPublishResponse post = postService.createPost(request, postUserId);
    log.info("-----user insert success postId: " + post.getId()+ ", recipeId"+ post.getRecipeId() + " " + post.getTitle());
    return new ResponseData<>(HttpStatus.CREATED.value(), "Post created successfully", post);
  }

  @PutMapping("/{id}")
  public ResponseData<PostPublishResponse> updatePost(@PathVariable Long id,  @RequestBody Map<String, String> requestBody) {
    String content = requestBody.get("content");
//    String status = requestBody.get("status");
    PostPublishResponse post = postService.updatePost(id, content);
    log.info("-----user update success postId: " + post.getId()+ ", recipeId"+ post.getRecipeId() + " " + post.getTitle());
    return new ResponseData<>(HttpStatus.OK.value(), "Post updated successfully", post);
  }

  @PostMapping(value = "/get-keyword", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseData<String> getKeyword(@RequestParam MultipartFile image) {
    String result = openAIImageService.extractKeywordsFromImage(image);
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

  @PutMapping("/admin/{id}")
  public ResponseData<PostPublishResponse> updatePostByAdmin(@PathVariable Long id,  @RequestBody Map<String, String> requestBody) {
    String content = requestBody.get("content");
    String status = requestBody.get("status");
    PostPublishResponse post = postService.updatePostByAdmin(id, content,status);
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
    @RequestParam(required = false) String region,
    @RequestParam(required = false) String difficulty,
    @RequestParam(required = false) String category,
    @RequestParam(required = false) Integer maxCookTime,
    @RequestParam(defaultValue = "1") int page,
    @RequestParam(defaultValue = "10") int size
  ) {
    Page<PostResponse> result = postService.getAllPosts(
      keyword,
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

  @GetMapping("/my")
  public ResponseData<Page<PostResponse>> getOwnPosts(
          @RequestParam Long userId,
          @RequestParam(defaultValue = "1") int page,
          @RequestParam(defaultValue = "10") int size,
          @RequestParam(defaultValue = "createdAt") String sortBy,
          @RequestParam(defaultValue = "DESC") Sort.Direction direction
  ) {
    log.info(userId.toString());
    Pageable pageable = PageRequest.of(page - 1, size, Sort.by(direction, sortBy));
    Page<PostResponse> result = postService.getOwnPosts(
            userId,
            pageable
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

  @GetMapping("/recipes/{recipeId}/similar")
  public ResponseData<List<SimilarPostResponse>> getSimilarRecipes(
    @PathVariable Long recipeId,
    @RequestParam(required = false) List<String> ingredients,
    @RequestParam(defaultValue = "10") Integer limit
  ) {
    log.info("ingredients = {}", ingredients);  
    List<SimilarPostResponse> posts = postService.getSimilarRecipes(recipeId, ingredients, limit);
    return new ResponseData<>(HttpStatus.OK.value(), "Success", posts);
  }
}
