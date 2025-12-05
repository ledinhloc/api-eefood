package com.eefood.iamservice.dto.request;

import com.eefood.iamservice.enums.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminUpdateRequest {
  private Long id;
  private Role role;
}
