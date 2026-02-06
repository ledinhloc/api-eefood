package com.eefood.reactionservice.livestream.dto.response;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ViewerUpdateMessage {
  private String type; // "JOIN" hoặc "LEAVE"
  private ViewerResponse viewer; // null nếu type = "LEAVE"
  private Long userId; // Chỉ có khi type = "LEAVE"
}