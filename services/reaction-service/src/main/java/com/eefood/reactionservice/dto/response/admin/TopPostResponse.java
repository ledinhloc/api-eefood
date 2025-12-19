package com.eefood.reactionservice.dto.response.admin;

import com.eefood.reactionservice.dto.response.UserInfo;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TopPostResponse {
    private Long postId;
    private String title;
    private String imageUrl;
    private UserInfo userInfo;
    private Long count;
    private LocalDateTime createdAt;
}
