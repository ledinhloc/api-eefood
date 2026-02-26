package com.eefood.reactionservice.service.chatbot;

import com.eefood.reactionservice.dto.response.chatbot.ChatbotResponse;
import dev.langchain4j.service.tool.ToolExecution;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatbotToolExecutor {

    @Transactional
    public ChatbotResponse execute(ToolExecution toolExecution) {
        try {
            Object result = toolExecution.resultObject();

            if (result instanceof ChatbotResponse response) {
                return response;
            }

            log.error("Tool returned unexpected type: {}", result);
            return ChatbotResponse.empty();

        } catch (Exception e) {
            log.error("Tool execution failed", e);
            return ChatbotResponse.empty();
        }
    }
}
