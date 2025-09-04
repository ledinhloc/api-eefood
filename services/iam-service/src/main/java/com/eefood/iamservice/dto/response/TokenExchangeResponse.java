package com.eefood.iamservice.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
//@JsonInclude(JsonInclude.Include.NON_NULL)
public class TokenExchangeResponse {
    String accessToken;
    Integer expiresIn;
    Integer refreshExpiresIn;
    String refreshToken;
    String tokenType;
    String idToken; // có thể null nếu không yêu cầu scope "openid"
    Integer notBeforePolicy;
    String sessionState;
    String scope;
}

