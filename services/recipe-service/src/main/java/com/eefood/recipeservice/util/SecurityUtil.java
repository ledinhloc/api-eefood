package com.eefood.recipeservice.util;

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

}
