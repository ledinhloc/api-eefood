package com.eefood.iamservice.dto.request;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.LocalDate;
import java.util.List;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserCreateRequest {
  @NotBlank
  String username;
  @Email(message = "Email is not valid")
  @NotBlank(message = "Email is required")
  String email;
  String role;
  LocalDate dob;
  String gender;
  JsonNode address; // luu json
  String provider;
  String avatarUrl;
  List<String> allergies;
  List<String> eatingPreferences;
  List<String> dietaryPreferences;
  @NotBlank(message = "Password is required")
  @Size(min = 8, message = "Password must be at least 8 characters")
  String password;
}