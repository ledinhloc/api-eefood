package com.eefood.notificationservice.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final WebSocketAuthInterceptor webSocketAuthInterceptor;
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Endpoint để client connect
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*") // Cho phép mọi domain connect (có thể set cụ thể)
                .addInterceptors(webSocketAuthInterceptor)
                .setHandshakeHandler(new CustomHandshakeHandler())
                .withSockJS(); // Hỗ trợ fallback SockJS (trường hợp trình duyệt ko hỗ trợ WS)
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // Prefix cho message mà client gửi lên server
        registry.setApplicationDestinationPrefixes("/app");

        // Prefix cho message mà server push ra client
        registry.enableSimpleBroker("/topic", "/queue");

        // Định danh user (khi gửi riêng lẻ)
        registry.setUserDestinationPrefix("/user");
    }
}
