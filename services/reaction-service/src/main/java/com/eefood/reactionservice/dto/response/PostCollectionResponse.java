package com.eefood.reactionservice.dto.response;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostCollectionResponse {
  private Long id;
  private Long postId;
  private Long userId;
  private Long recipeId;
  private String title;
  private String imageUrl;
  private LocalDateTime createdAt;
  private String username;
  private String email;
  private String avatarUrl;
}
