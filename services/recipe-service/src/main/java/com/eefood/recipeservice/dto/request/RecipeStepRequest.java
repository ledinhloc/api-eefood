package com.eefood.recipeservice.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecipeStepRequest {
  private Long id;              // null nếu là step mới
  private Integer stepNumber;
  private String instruction;
  private String imageUrl;
  private String videoUrl;
  private Integer stepTime;
}