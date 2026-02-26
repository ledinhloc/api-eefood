package com.eefood.reactionservice.config;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class FeignClientConfig {
    @Bean
    public RequestInterceptor requestInterceptor() {
        return new RequestInterceptor() {
            @Override
            public void apply(RequestTemplate template) {
                Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                if (authentication instanceof JwtAuthenticationToken jwtAuth) {
                    String token = jwtAuth.getToken().getTokenValue();
                    template.header("Authorization", "Bearer " + token);
                }
                else {
                    log.warn("No JWT authentication found for Feign request to: {}",
                            template.url());
                }
            }
        };
    }
}
