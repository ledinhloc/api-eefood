package com.eefood.reactionservice.service.chatbot;

import com.eefood.reactionservice.dto.response.chatbot.ChatbotResponse;
import dev.langchain4j.data.message.Content;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import org.springframework.cache.annotation.Cacheable;

import java.util.List;

public interface ChatbotAIService {
    @SystemMessage(fromResource = "prompts/chatbot_system_prompt.txt")
    ChatbotResponse chat(
            @UserMessage String userMessage
    );

    @SystemMessage(fromResource = "prompts/chatbot_system_prompt.txt")
    TokenStream chatStream(@UserMessage String userMessage);
}
