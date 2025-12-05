package com.eefood.iamservice.config;

import java.util.Optional;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

@Component("auditorAware")
public class AuditorAwareImpl implements AuditorAware<String> {
  @Override
  public Optional<String> getCurrentAuditor() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null || !authentication.isAuthenticated()) {
      return Optional.of("System");
    }

    // Nếu là JWT (Keycloak + OAuth2 Resource Server)
    if (authentication instanceof JwtAuthenticationToken jwtAuth) {
      Jwt jwt = jwtAuth.getToken();
      String username = jwt.getClaimAsString("preferred_username");
      return Optional.ofNullable(username != null ? username : jwt.getSubject());
    }

    // Các loại Authentication khác (vd: BasicAuth)
    return Optional.of(authentication.getName());
  }
}


