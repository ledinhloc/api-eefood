package com.eefood.iamservice.dto.request;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserCreateRequest {
  String username;
  String email;
  String role;
  LocalDate dob;
  String gender;
  JsonNode address; // luu json
  String provider;
  String avatarUrl;
  List<String> allergies;
  List<String> eatingPreferences;
  String password;
}
