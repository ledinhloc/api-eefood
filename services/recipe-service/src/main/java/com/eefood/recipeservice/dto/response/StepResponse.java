package com.eefood.recipeservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StepResponse {
  private Long id;
  private Integer stepNumber;
  private String instruction;
  private String imageUrl;
  private String videoUrl;
  private Integer stepTime;
}