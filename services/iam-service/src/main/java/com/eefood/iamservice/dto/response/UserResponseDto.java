package com.eefood.iamservice.dto.response;

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
public class UserResponseDto {
  private Long id;
  private String username;
  private String email;
  private String role;
  private LocalDate dob;
  private String gender;
  private String address;
  private String provider;
  private String avatarUrl;
  private List<String> allergies;
  private List<String> eatingPreferences;
}