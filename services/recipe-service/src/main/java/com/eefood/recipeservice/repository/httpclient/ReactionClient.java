package com.eefood.recipeservice.repository.httpclient;

import com.eefood.recipeservice.dto.request.PostCreateRequest;
import com.eefood.recipeservice.dto.response.PostPublishResponse;
import com.eefood.recipeservice.dto.response.ResponseData;
import com.eefood.recipeservice.dto.response.UserInfo;
import java.util.List;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "reaction-service")
public interface ReactionClient {
  @PostMapping("/api/v1/posts/import")
  ResponseData<PostPublishResponse> createPost(
    @RequestBody PostCreateRequest request,
    @RequestParam Long userId
  );
}
