package com.eefood.iamservice.dto.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TokenExchangeParam {
    String grant_type;
    String client_id;
    String client_secret;
    String username;
    String password;
    String scope;
    String refresh_token;
    String subject_token;
}
