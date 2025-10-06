package com.eefood.notificationservice.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

@Component
@Slf4j
@RequiredArgsConstructor
public class WebSocketAuthInterceptor implements HandshakeInterceptor {
    private final JwtDecoder jwtDecoder;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {
        if (request instanceof ServletServerHttpRequest servletRequest) {
            String authHeader = servletRequest.getServletRequest().getHeader("Authorization");

            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);

                try {
                    // Spring Security validate + decode token
                    Jwt jwt = jwtDecoder.decode(token);

                    // Lấy custom claim "userId" (do bạn cấu hình trong Keycloak)
                    String userId = jwt.getClaimAsString("userId");

                    if (userId != null) {
                        attributes.put("userId", userId);
                        log.info("✅ WebSocket connection authenticated for userId={}", userId);
                        return true;
                    } else {
                        log.warn("Token does not contain userId claim");
                    }
                } catch (Exception e) {
                    log.warn("Invalid JWT token: {}", e.getMessage());
                }
            } else {
                log.warn("Missing or invalid Authorization header");
            }
        }
        return false;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
        // Không cần xử lý sau handshake
    }
}
