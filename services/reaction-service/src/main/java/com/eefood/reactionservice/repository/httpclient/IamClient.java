package com.eefood.reactionservice.repository.httpclient;

import org.springframework.cloud.openfeign.FeignClient;

@FeignClient(name = "iam-service")
public interface IamClient {}
