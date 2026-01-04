package com.eefood.reactionservice.dto.response;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApprovePostResponse {
  private Long id;
  private Long postId;
  private Long recipeId;
  private Long userId;

  private String status;
  private String summary;
  private Double totalScore;

  private Integer recipeCompleteness;
  private Integer ingredientSafety;
  private Integer stepClarity;
  private Integer contentAppropriate;
  private Integer contentRelevance;
  private Integer mediaQuality;

  private String completenessNote;
  private String safetyNote;
  private String clarityNote;
  private String appropriatenessNote;
  private String relevanceNote;
  private String mediaQualityNote;

  private LocalDateTime createdAt;
}
