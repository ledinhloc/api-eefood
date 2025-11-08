package com.eefood.reactionservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class FollowResponse {

    private Long id;
    private Long followerId;
    private Long followingId;
    private LocalDateTime createdAt;

    //  Thông tin user
    private boolean isFollow;
    private String username;
    private String email;
    private String avatarUrl;

}
