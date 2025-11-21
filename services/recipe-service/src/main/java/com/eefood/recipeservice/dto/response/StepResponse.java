package com.eefood.recipeservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StepResponse {
  private Long id;
  private Integer stepNumber;
  private String instruction;
  private List<String> imageUrls;
  private List<String> videoUrls;
  private Integer stepTime;
}