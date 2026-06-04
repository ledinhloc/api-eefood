package com.eefood.reactionservice.livestream.repository.httpclient;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

@FeignClient(name = "subtitle-worker", url = "${subtitle-worker.base-url:http://127.0.0.1:9000}")
public interface SubtitleWorkerClient {
  @PostMapping("/start")
  void start(@RequestBody Map<String, Object> request);

  @PostMapping("/stop")
  void stop(@RequestBody Map<String, Object> request);
}
