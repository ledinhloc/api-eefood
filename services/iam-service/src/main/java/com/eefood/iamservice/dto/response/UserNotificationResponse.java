package com.eefood.iamservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserNotificationResponse {
    private Long id;
    private String email;
    private String username;
    private String role;
}
