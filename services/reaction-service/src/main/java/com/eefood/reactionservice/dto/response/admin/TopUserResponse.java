package com.eefood.reactionservice.dto.response.admin;

import com.eefood.reactionservice.dto.response.UserInfo;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TopUserResponse {
    private UserInfo userInfo;
    private Long followerCount;
}
