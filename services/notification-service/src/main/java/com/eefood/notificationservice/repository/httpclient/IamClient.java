package com.eefood.notificationservice.repository.httpclient;

import com.eefood.notificationservice.config.FeignClientConfig;
import com.eefood.notificationservice.dto.request.UserNotificationResquest;
import com.eefood.notificationservice.dto.response.ResponseData;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@FeignClient(name = "iam-service", configuration = FeignClientConfig.class)
public interface IamClient {
    @GetMapping(value = "/api/v1/users/all")
    ResponseData<List<UserNotificationResquest>> getAllUserNotifications();
}
