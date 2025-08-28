package com.eefood.iamservice.repository.httpclient;

import com.eefood.iamservice.dto.request.TokenExchangeParam;
import com.eefood.iamservice.dto.request.UserCreationParam;
import com.eefood.iamservice.dto.response.TokenExchangeResponse;
import feign.QueryMap;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

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

  @PostMapping(value = "/admin/realms/{realm}/users", consumes = MediaType.APPLICATION_JSON_VALUE)
  ResponseEntity<?> createUser(@RequestHeader("authorization") String token,
                               @PathVariable("realm") String realm,
                               @RequestBody UserCreationParam param);
}
