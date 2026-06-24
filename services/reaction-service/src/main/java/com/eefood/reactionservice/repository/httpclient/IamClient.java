package com.eefood.reactionservice.repository.httpclient;

import com.eefood.reactionservice.dto.request.UserNotificationResquest;
import com.eefood.reactionservice.dto.response.UserBodyMetricsResponse;
import com.eefood.reactionservice.dto.response.ResponseData;
import com.eefood.reactionservice.dto.response.UserInfo;
import com.eefood.reactionservice.dto.response.UserHeightResponse;
import com.eefood.reactionservice.dto.response.UserResponse;
import com.eefood.reactionservice.dto.response.UserWeightResponse;
import com.eefood.reactionservice.dto.response.admin.UserCityStatisticsResponse;
import com.eefood.reactionservice.dto.response.admin.UserRegistrationStatsResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.util.List;

@FeignClient(name = "iam-service")
public interface IamClient {
  @GetMapping("/api/v1/users/info/{userId}")
  ResponseData<UserResponse> getUserById(@PathVariable("userId") Long userId);

  @GetMapping("/api/v1/users/{userId}")
  ResponseData<UserInfo> getUserInfo(@PathVariable("userId") Long userId);

  @GetMapping("/api/v1/users/{userId}/body-metrics")
  ResponseData<UserBodyMetricsResponse> getUserBodyMetrics(@PathVariable("userId") Long userId);

  @GetMapping("/api/v1/users/{userId}/weights")
  ResponseData<List<UserWeightResponse>> getUserWeights(
      @PathVariable("userId") Long userId,
      @RequestParam("from") LocalDate from,
      @RequestParam("to") LocalDate to);

  @GetMapping("/api/v1/users/{userId}/heights")
  ResponseData<List<UserHeightResponse>> getUserHeights(
      @PathVariable("userId") Long userId,
      @RequestParam("from") LocalDate from,
      @RequestParam("to") LocalDate to);

  @PostMapping("/api/v1/users/batch")
  ResponseData<List<UserInfo>> getUserInfoBatch(@RequestBody List<Long> userIds);

  @GetMapping(value = "/api/v1/users/all")
  ResponseData<List<UserNotificationResquest>> getAllUserNotifications();

  @GetMapping(value = "/api/v1/users/dashboard/recent")
  ResponseData<List<UserRegistrationStatsResponse>> getRecentUsers();

  @GetMapping("/api/v1/users/dashboard/city")
  ResponseData<List<UserCityStatisticsResponse>> getUserStatisticsByCity();
}
