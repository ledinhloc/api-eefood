package com.eefood.iamservice.dto.response;

import com.eefood.iamservice.enums.Gender;
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
public class UserUpdateResponse {
    Long id;
    String username;
    String email;
    List<String> allergies;
    List<String> eatingPreferences;
    List<String> dietaryPreferences;
}
