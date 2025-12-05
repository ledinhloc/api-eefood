package com.eefood.reactionservice.dto.request;

import lombok.*;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PostCollectionsRequest {
  private Long postId;
  private List<Long> collectionIds;
}