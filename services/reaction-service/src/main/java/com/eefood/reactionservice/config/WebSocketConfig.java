package com.eefood.reactionservice.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

  @Override
  public void configureMessageBroker(MessageBrokerRegistry config) {
    // Enable simple broker for topics
    config.enableSimpleBroker("/topic");

    // Application destination prefix
    config.setApplicationDestinationPrefixes("/app");
  }

  @Override
  public void registerStompEndpoints(StompEndpointRegistry registry) {
    // Register STOMP endpoint
    registry.addEndpoint("/ws/livestream")
      .setAllowedOriginPatterns("*")
      .withSockJS();
  }
}

/*
 * WebSocket Topics cho Live Stream:
 *
 * 1. Comments (hiển thị 5 message mới nhất):
 *    /topic/livestream/{liveStreamId}/comments
 *
 * 2. Reactions (thả tim):
 *    /topic/livestream/{liveStreamId}/reactions
 *
 * 3. Viewer count (số người xem):
 *    /topic/livestream/{liveStreamId}/viewers
 *
 *
 * Flutter client sẽ subscribe các topics này để nhận real-time updates
 */