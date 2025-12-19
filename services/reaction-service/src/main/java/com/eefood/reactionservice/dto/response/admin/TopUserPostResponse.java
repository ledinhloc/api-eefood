package com.eefood.reactionservice.dto.response.admin;

import com.eefood.reactionservice.dto.response.UserInfo;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TopUserPostResponse {
    private UserInfo userInfo;
    private Long postCount;
}
