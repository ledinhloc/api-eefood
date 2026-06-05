package com.eefood.reactionservice.livestream.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LeaderboardEntryResponse {
    private int    rank;
    private Long   userId;
    private String username;
    private String avatarUrl;
    private long   totalDiamonds;
}
