package com.eefood.recipeservice.dto.request;

import lombok.*;
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostCreateRequest {
  private Long recipeId;
  private String content;
}
