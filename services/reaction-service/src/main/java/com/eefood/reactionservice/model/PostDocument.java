package com.eefood.reactionservice.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;
import org.springframework.data.annotation.Id;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostDocument {
  @Id
  private Long id;
  private Long userId;
  private String title;
  private String content;
  private String imageUrl;

  private String description;
  private String region;
  private Integer prepTime;
  private Integer cookTime;
  private String difficulty;
  private String status;

  private Set<String> recipeCategories = new HashSet<>();
  private Set<String> recipeIngredientKeywords = new HashSet<>();

  private Long totalReactionCount;
  private Long totalShares;

  private LocalDateTime createdAt;
}