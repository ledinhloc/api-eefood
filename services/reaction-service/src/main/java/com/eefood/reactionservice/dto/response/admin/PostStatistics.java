package com.eefood.reactionservice.dto.response.admin;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostStatistics {
    private List<TopPostResponse> topLikedPosts;
    private Long totalViolatedPosts;
    private List<ViolatedPostResponse> recentViolatedPosts;
}
