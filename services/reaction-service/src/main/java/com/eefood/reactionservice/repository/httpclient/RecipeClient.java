package com.eefood.reactionservice.repository.httpclient;

import org.springframework.cloud.openfeign.FeignClient;

@FeignClient(name = "recipe-service")
public interface RecipeClient {

}
