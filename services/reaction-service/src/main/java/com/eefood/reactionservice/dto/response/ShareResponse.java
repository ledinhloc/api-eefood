package com.eefood.reactionservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShareResponse {
    private Long id;
    private Long postId;
    private String content;
    private Long userId;
    private String platform;
    private String imageUrl;
}
