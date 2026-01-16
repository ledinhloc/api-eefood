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
    String chatRole;
    String chatTool;
    String message;
    String imageUrl;
    String weather;
    String location;
    Long userId;
}
