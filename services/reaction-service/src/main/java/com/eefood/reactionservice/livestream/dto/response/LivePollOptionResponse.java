package com.eefood.reactionservice.livestream.dto.response;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class LivePollOptionResponse {
  private Long id;
  private String text;
  private Long count;
}