package com.eefood.reactionservice.dto.response;

import com.eefood.reactionservice.enums.Difficulty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class RecipeSummaryResponse {
  private Long id;
  private String title;
  private String description;
  private String region;
  private String imageUrl;
  private String videoUrl;
  private Integer prepTime;
  private Integer cookTime;
  private Difficulty difficulty;
}