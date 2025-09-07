package com.eefood.iamservice.dto.request;

import com.eefood.iamservice.enums.Gender;
import com.eefood.iamservice.enums.Role;
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
