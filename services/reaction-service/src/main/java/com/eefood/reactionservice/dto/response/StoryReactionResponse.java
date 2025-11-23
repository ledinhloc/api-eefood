package com.eefood.reactionservice.dto.response;

import com.eefood.reactionservice.enums.ReactionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StoryReactionResponse {
    private Long id;
    private Long storyId;
    private Long userId;
    private String username;
    private String avatarUrl;
    private ReactionType reactionType;
    private LocalDateTime createdAt;
}
