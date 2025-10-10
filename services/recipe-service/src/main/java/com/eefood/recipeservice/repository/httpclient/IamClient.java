package com.eefood.recipeservice.repository.httpclient;

import java.util.List;

import com.eefood.recipeservice.dto.response.ResponseData;
import com.eefood.recipeservice.dto.response.UserInfo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "iam-service")
public interface IamClient {
  @GetMapping("/api/v1/users/{userId}")
  ResponseData<UserInfo> getUserInfo(@PathVariable("userId") Long userId);

  @PostMapping("/api/v1/users/batch")
  ResponseData<List<UserInfo>> getUserInfoBatch(@RequestBody List<Long> userIds);
}
