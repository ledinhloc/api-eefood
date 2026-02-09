package com.eefood.reactionservice.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatBotRequest {
    private String chatRole;
    private String message;
    private String imageUrl;
    private LocationInfoRequest location;
    private String time;
    private List<Long> postId;
    private List<Long> recipeId;
    private Long userId;
}
