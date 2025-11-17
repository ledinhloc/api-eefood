package com.eefood.reactionservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class StoryResponse {
    private Long id;
    private Long userId;
    private String type;
    private String contentUrl;
    private LocalDateTime createdAt;
    private LocalDateTime expiredAt;
}
