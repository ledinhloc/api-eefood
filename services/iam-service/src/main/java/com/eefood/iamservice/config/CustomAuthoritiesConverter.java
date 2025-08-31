package com.eefood.iamservice.config;

import java.util.*;
import java.util.stream.Collectors;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

public class CustomAuthoritiesConverter implements Converter<Jwt, Collection<GrantedAuthority>> {
  private final String REALM_ACCESS = "realm_access";
  private final String RESOURCE_ACCESS = "resource_access";
  private final String ROLE_PREFIX = "ROLE_";
  @Override
  public Collection<GrantedAuthority> convert(Jwt jwt) {
    Collection<GrantedAuthority> authorities = new ArrayList<>();

    Map<String, Object> realmAccess = jwt.getClaim(REALM_ACCESS);
    if (realmAccess != null && realmAccess.containsKey("roles")) {
      List<String> roles = (List<String>) realmAccess.get("roles");
      authorities.addAll(
              roles.stream()
                      .map(role -> new SimpleGrantedAuthority(ROLE_PREFIX + role.toUpperCase()))
                      .toList()
      );
    }

    // lấy client roles nếu cần
    Map<String, Object> resourceAccess = jwt.getClaim(RESOURCE_ACCESS);
    if (resourceAccess != null) {
      resourceAccess.values().forEach(obj -> {
        Map<String, Object> client = (Map<String, Object>) obj;
        List<String> roles = (List<String>) client.get("roles");
        if (roles != null) {
          authorities.addAll(
                  roles.stream()
                          .map(role -> new SimpleGrantedAuthority(ROLE_PREFIX + role.toUpperCase()))
                          .toList()
          );
        }
      });
    }

    return authorities;
  }
}
