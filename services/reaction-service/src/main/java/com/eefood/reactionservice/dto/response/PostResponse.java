package com.eefood.reactionservice.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostResponse {
  private Long id;
  private Long userId;
  private Long recipeId;
  private String title;
  private String content;
  private String imageUrl;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;

  private Long totalShares;
  private Map<String, Long> reactionCounts; // ví dụ: {"LIKE": 10, "LOVE": 3}
  private List<CommentResponse> comments;

  private String username;
  private String email;
  private String avatarUrl;
}
