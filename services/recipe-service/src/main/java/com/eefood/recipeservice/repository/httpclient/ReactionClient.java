package com.eefood.recipeservice.repository.httpclient;

import com.eefood.recipeservice.dto.request.PostCreateRequest;
import com.eefood.recipeservice.dto.response.PostPublishResponse;
import com.eefood.recipeservice.dto.response.ResponseData;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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

  @PostMapping(
          value = "/api/v1/posts/get-keyword",
          consumes = MediaType.MULTIPART_FORM_DATA_VALUE
  )
  ResponseData<String> getKeyword(@RequestPart("image") MultipartFile image);

}
