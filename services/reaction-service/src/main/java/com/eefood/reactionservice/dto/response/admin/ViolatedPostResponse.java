package com.eefood.reactionservice.dto.response.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ViolatedPostResponse {
    private Long postId;
    private String title;
    private String content;
    private Long userId;
    private String username;
    private String reason;
}
