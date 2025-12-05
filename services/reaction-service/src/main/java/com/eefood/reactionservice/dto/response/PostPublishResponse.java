package com.eefood.reactionservice.dto.response;

import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class PostPublishResponse {
  private Long id;
  private Long recipeId;
  private Long userId;
  private String title;
  private String content;
  private String imageUrl;
  private LocalDateTime createdAt;

  // Thông tin hiển thị
  private String difficulty;
  private String location;
  private String prepTime;
  private String cookTime;
  private Long countReaction;
  private Long countComment;
  private String status;
}
