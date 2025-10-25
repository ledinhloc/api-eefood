package com.eefood.reactionservice.dto.response;
import com.fasterxml.jackson.annotation.JsonInclude;
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
  private LocalDateTime updatedAt;
  private List<PostCollectionResponse> posts;
}