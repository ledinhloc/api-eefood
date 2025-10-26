package com.eefood.reactionservice.model;

import lombok.*;
import org.springframework.data.annotation.Id;

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
}