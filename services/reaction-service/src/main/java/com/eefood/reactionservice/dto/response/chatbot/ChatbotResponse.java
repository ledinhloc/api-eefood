package com.eefood.reactionservice.dto.response.chatbot;

import com.eefood.reactionservice.enums.ChatRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ChatbotResponse {
    private Long id;
    private String message;
    private String role;
    private List<Object> data;
    private Map<String, Object> meta;

    public static ChatbotResponse empty() {
        return ChatbotResponse.builder()
                .message("")
                .role(ChatRole.AI.name())
                .data(new ArrayList<>())
                .meta(new HashMap<>())
                .build();
    }
}
