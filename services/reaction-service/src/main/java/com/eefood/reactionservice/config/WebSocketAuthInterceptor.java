package com.eefood.reactionservice.config;

import java.util.Map;
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

@Component
@Slf4j
@RequiredArgsConstructor
public class WebSocketAuthInterceptor implements HandshakeInterceptor {
    private final JwtDecoder jwtDecoder;

  @Override
  public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                 WebSocketHandler wsHandler, Map<String, Object> attributes) {
    if (request instanceof ServletServerHttpRequest servletRequest) {
      String token = null;

      //  Thử lấy từ Header trước (cho web client)
      String authHeader = servletRequest.getServletRequest().getHeader("Authorization");
      if (authHeader != null && authHeader.startsWith("Bearer ")) {
        token = authHeader.substring(7);
//        log.info("token da co trong header: {}", token);
      }else {
        log.info("token chua cho trong header: {}", token);
      }


      // Nếu không có trong header, lấy từ query param (cho mobile)
      if (token == null) {
        String queryString = servletRequest.getServletRequest().getQueryString();
        if (queryString != null && queryString.contains("token=")) {
          // Parse query string để lấy token
          String[] params = queryString.split("&");
          for (String param : params) {
            if (param.startsWith("token=")) {
              token = param.substring(6); // Bỏ "token="
              break;
            }
          }
        }
      }

//      log.info("token queryString: {}", token);


      // 3. Validate token
      if (token != null) {
        try {
          Jwt jwt = jwtDecoder.decode(token);
          String userId = jwt.getClaimAsString("userId");

          if (userId != null) {
            attributes.put("userId", userId);
            log.info("WebSocket authenticated for userId={}", userId);
            return true;
          } else {
            log.warn("Token không có userId claim");
          }
        } catch (Exception e) {
          log.warn("Invalid JWT token: {}", e.getMessage());
        }
      } else {
        log.warn("Không tìm thấy token trong header hoặc query param");
      }
    }

    return false; // Reject connection nếu không auth được
  }
    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
        // Không cần xử lý sau handshake
    }
}
