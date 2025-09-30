package com.eefood.notificationservice.utils;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

@Component
public class SecurityUtil {
  public Long getCurrentUserId() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt)) {
      throw new RuntimeException("Unauthorized");
    }
    return jwt.getClaim("userId");
  }

  public boolean hasRole(String role) {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null) return false;

    return authentication.getAuthorities().stream()
      .anyMatch(auth -> auth.getAuthority().equals("ROLE_" + role));
  }

}
