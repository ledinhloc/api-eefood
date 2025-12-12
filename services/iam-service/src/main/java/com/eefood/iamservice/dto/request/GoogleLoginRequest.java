package com.eefood.iamservice.dto.request;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GoogleLoginRequest {
    @NotBlank(message = "ID token is required")
    private String idToken;

    private String fcmToken;
}
