package com.eefood.reactionservice.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatBotRequest {
    private String chatRole;
    private String chatTool;
    private String message;
    private String imageUrl;
    private LocationInfoRequest location;
    private String time;
    Long userId;
}
