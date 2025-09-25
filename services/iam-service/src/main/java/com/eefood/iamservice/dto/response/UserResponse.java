package com.eefood.iamservice.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.LocalDate;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Builder
public class UserResponse {
  private Long id;
  private String username;
  private String email;
  private String role;
  private LocalDate dob;
  private String gender;
  private JsonNode address;
  private String provider;
  private String avatarUrl;
  private List<String> allergies;
  private List<String> eatingPreferences;
  private List<String> dietaryPreferences;
  private String accessToken;
  private String refreshToken;
}
