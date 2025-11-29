package com.eefood.reactionservice.dto.response;

import com.eefood.reactionservice.enums.Difficulty;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostResponse {
  private Long id;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;

  //Post
  private String status;
  private String content;
  private Long totalShares;
  private Map<String, Long> reactionCounts; // ví dụ: {"LIKE": 10, "LOVE": 3}
//  private List<CommentResponse> comments;

  private Long userId;
  private String username;
  private String email;
  private String avatarUrl;

  // ====== thong tin recipe ===========
  private Long recipeId;
  private String title;
  private String description;
  private String region;
  private String imageUrl;
  private Integer prepTime;
  private Integer cookTime;
  @Enumerated(EnumType.STRING)
  @Column(length = 7)
  private Difficulty difficulty;

  private Set<String> recipeCategories;
  private Set<String> recipeIngredientKeywords;

}
