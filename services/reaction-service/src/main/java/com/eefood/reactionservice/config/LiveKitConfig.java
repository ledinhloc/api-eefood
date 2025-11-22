package com.eefood.reactionservice.config;

import lombok.Getter;
import org.springframework.context.annotation.Configuration;
import io.livekit.server.RoomServiceClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
@Configuration
@Getter
public class LiveKitConfig {
  @Value("${livekit.api.key}")
  private String apiKey;

  @Value("${livekit.api.secret}")
  private String apiSecret;

  @Value("${livekit.ws.url}")
  private String wsUrl;

  @Value("${livekit.http.url}")
  private String httpUrl;

  @Bean
  public RoomServiceClient roomServiceClient() {
    return RoomServiceClient.create(httpUrl, apiKey, apiSecret);
  }
}
