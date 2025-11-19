package com.eefood.reactionservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class StoryViewResponse {
    private Long id;

    private Long userId;
    private String username;
    private String email;
    private String avatarUrl;
}
