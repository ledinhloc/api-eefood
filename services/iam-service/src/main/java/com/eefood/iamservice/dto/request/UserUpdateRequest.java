package com.eefood.iamservice.dto.request;

import com.eefood.iamservice.enums.Gender;
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
@Builder
public class UserUpdateRequest {
  Long id;
  String username;
  String email;
  LocalDate dob;
  Gender gender;
  JsonNode address;
  String avatarUrl;
  List<String> allergies;
  List<String> eatingPreferences;
}
