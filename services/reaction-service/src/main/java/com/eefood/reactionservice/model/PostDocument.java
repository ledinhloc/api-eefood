package com.eefood.reactionservice.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(indexName = "posts")
public class PostDocument {
  @Id
  private Long id;
  private Long userId;
  private String title;
  private String content;
  private String imageUrl;
}