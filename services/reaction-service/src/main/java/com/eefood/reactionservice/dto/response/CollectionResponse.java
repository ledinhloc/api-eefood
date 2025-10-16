package com.eefood.reactionservice.dto.response;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CollectionResponse {
  private Long id;
  private String name;
  private String coverImageUrl;
  private LocalDateTime createdAt;
  private List<PostResponse> posts;
}