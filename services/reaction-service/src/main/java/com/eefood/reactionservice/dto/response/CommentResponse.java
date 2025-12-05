package com.eefood.reactionservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommentResponse {
  private Long id;
  private Long userId;
  private Long parentId;
  private String content;
  private LocalDateTime createdAt;
  private List<CommentResponse> replies;
  private Map<String, Long> reactionCounts;
  private Integer replyCount;
  private List<String> images;
  private List<String> videos;

  private String username;
  private String email;
  private String avatarUrl;
}