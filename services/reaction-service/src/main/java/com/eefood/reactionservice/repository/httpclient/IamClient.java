package com.eefood.reactionservice.repository.httpclient;

import com.eefood.reactionservice.dto.response.ResponseData;
import com.eefood.reactionservice.dto.response.UserInfo;
import com.eefood.reactionservice.dto.response.UserResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@FeignClient(name = "iam-service")
public interface IamClient {
  @GetMapping("/api/v1/users/info/{userId}")
  ResponseData<UserResponse> getUserById(@PathVariable("userId") Long userId);

  @GetMapping("/api/v1/users/{userId}")
  ResponseData<UserInfo> getUserInfo(@PathVariable("userId") Long userId);

  @PostMapping("/api/v1/users/batch")
  ResponseData<List<UserInfo>> getUserInfoBatch(@RequestBody List<Long> userIds);
}
