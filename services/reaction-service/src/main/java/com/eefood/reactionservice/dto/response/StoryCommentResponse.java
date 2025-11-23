package com.eefood.reactionservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StoryCommentResponse {
    private Long id;
    private Long storyId;
    private Long userId;
    private String message;
    private LocalDateTime createdAt;

    private String username;
    private String email;
    private String avatarUrl;

    private Long parentId;
}
