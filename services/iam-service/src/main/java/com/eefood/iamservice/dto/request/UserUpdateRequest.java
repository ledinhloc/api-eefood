package com.eefood.iamservice.dto.request;

import com.eefood.iamservice.enums.Gender;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.LocalDate;
import java.util.List;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserUpdateRequest {
  Long id;
  @NotBlank
  String username;
  @Email(message = "Email is not valid")
  @NotBlank(message = "Email is required")
  String email;
  LocalDate dob;
  Gender gender;
  JsonNode address;
  String avatarUrl;
  String backgroundUrl;
  List<String> allergies;
  List<String> eatingPreferences;
  List<String> dietaryPreferences;
}