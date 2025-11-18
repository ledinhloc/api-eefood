package com.eefood.recipeservice.dto.response;

import java.time.LocalDateTime;
import lombok.*;
import lombok.experimental.SuperBuilder;

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
}
