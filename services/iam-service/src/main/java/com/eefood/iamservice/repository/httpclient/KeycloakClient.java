package com.eefood.iamservice.repository.httpclient;

import com.eefood.iamservice.dto.request.Credential;
import com.eefood.iamservice.dto.request.TokenExchangeParam;
import com.eefood.iamservice.dto.request.UserCreationParam;
import com.eefood.iamservice.dto.response.TokenExchangeResponse;
import feign.QueryMap;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@FeignClient(name = "identity-client", url = "${idp.url}")
public interface KeycloakClient
{
  //lay token keycloak
  @PostMapping(
    value = "/realms/{realm}/protocol/openid-connect/token",
    consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
  TokenExchangeResponse exchangeToken(
    @PathVariable("realm") String realm,
    @QueryMap TokenExchangeParam param);

  // Tạo user (admin)
  @PostMapping(value = "/admin/realms/{realm}/users", consumes = MediaType.APPLICATION_JSON_VALUE)
  ResponseEntity<?> createUser(@RequestHeader("authorization") String token,
                               @PathVariable("realm") String realm,
                               @RequestBody UserCreationParam param);

  // Cập nhật user representation (PUT)
  @PutMapping(value = "/admin/realms/{realm}/users/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
  ResponseEntity<Void> updateUser(@RequestHeader("Authorization") String token,
                                  @PathVariable("realm") String realm,
                                  @PathVariable("id") String id,
                                  @RequestBody Map<String, Object> userRepresentation);

  // Tìm users theo email (GET /admin/realms/{realm}/users?email=...)
  @GetMapping(value = "/admin/realms/{realm}/users", produces = MediaType.APPLICATION_JSON_VALUE)
  List<Map<String, Object>> findUsersByEmail(@RequestHeader("Authorization") String token,
                                             @PathVariable("realm") String realm,
                                             @RequestParam("email") String email);

  // Lấy user representation theo id
  @GetMapping(value = "/admin/realms/{realm}/users/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
  Map<String, Object> getUserById(@RequestHeader("Authorization") String token,
                                  @PathVariable("realm") String realm,
                                  @PathVariable("id") String id);

  // Reset password (PUT /admin/realms/{realm}/users/{id}/reset-password)
  @PutMapping(value = "/admin/realms/{realm}/users/{id}/reset-password", consumes = MediaType.APPLICATION_JSON_VALUE)
  ResponseEntity<Void> resetPassword(@RequestHeader("Authorization") String token,
                                     @PathVariable("realm") String realm,
                                     @PathVariable("id") String id,
                                     @RequestBody Credential payload);
}
