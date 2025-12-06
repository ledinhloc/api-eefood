package com.eefood.recipeservice.dto.response;

import com.eefood.recipeservice.enums.ModerationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModerationResult {
  private ModerationStatus status;
  private String reason;
  private Double confidence;
}
