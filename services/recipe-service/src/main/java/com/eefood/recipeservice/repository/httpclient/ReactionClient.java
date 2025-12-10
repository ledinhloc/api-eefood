package com.eefood.recipeservice.repository.httpclient;

import com.eefood.recipeservice.dto.request.PostCreateRequest;
import com.eefood.recipeservice.dto.response.PostPublishResponse;
import com.eefood.recipeservice.dto.response.ResponseData;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "reaction-service")
public interface ReactionClient {
  @PostMapping("/api/v1/posts/import")
  ResponseData<PostPublishResponse> createPost(
    @RequestBody PostCreateRequest request,
    @RequestParam Long userId
  );

  @GetMapping("/api/v1/posts/user")
  ResponseData<List<PostPublishResponse>> getPostsPublishByUser();
}
