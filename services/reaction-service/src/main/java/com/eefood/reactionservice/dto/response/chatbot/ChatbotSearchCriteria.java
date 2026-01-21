package com.eefood.reactionservice.dto.response.chatbot;

import lombok.*;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatbotSearchCriteria {
    private String tool;
    private String keyword;
    private String difficulty;
    private List<String> category;
    private Integer maxCookTime;
    private String weather;
    private String location;
    private List<String> ingredient;
    private String timeOfDay;
    private String occasion;
    private boolean useImage;
}
