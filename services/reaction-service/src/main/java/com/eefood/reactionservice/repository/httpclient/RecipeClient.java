package com.eefood.reactionservice.repository.httpclient;

import com.eefood.reactionservice.dto.response.RecipeSummaryResponse;
import com.eefood.reactionservice.dto.response.ResponseData;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "recipe-service")
public interface RecipeClient {
  @GetMapping("/api/v1/recipes/search-ids")
  ResponseData<List<Long>> searchRecipeIds(
    @RequestParam(required = false) String keyword,
    @RequestParam(required = false) String region,
    @RequestParam(required = false) String difficulty
  );

  @GetMapping("/api/v1/recipes/summary/{id}")
  ResponseData<RecipeSummaryResponse> getRecipeSummary(@PathVariable("id") Long id);
}