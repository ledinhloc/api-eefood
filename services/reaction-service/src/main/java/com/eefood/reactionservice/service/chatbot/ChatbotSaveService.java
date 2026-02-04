package com.eefood.reactionservice.service.chatbot;

import com.eefood.reactionservice.dto.request.ChatBotRequest;
import com.eefood.reactionservice.dto.response.chatbot.ChatbotResponse;
import com.eefood.reactionservice.enums.ChatRole;
import com.eefood.reactionservice.enums.ChatTool;
import com.eefood.reactionservice.model.chatbot.ChatMessage;
import com.eefood.reactionservice.repository.chatbot.ChatbotRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatbotSaveService {
    private final ObjectMapper objectMapper;
    private final ChatbotRepository chatbotRepository;

    @Transactional
    public void saveChatForAI(ChatBotRequest chatBotRequest, ChatbotResponse chatbotResponse ,JsonNode output, Integer token, String tool) {
        ChatMessage chatMessage = ChatMessage.builder()
                .role(ChatRole.AI)
                .inputImageUrl("")
                .inputText(chatbotResponse.getMessage())
                .chatTool(ChatTool.valueOf(tool))
                .tokenUsage(token)
                .userId(chatBotRequest.getUserId())
                .outputJson(output)
                .build();

        chatbotRepository.save(chatMessage);
    }

    @Async
    @Transactional
    public void saveForAIAsync(ChatBotRequest request, ChatbotResponse response) {
        try {
            JsonNode outputJson = objectMapper.valueToTree(response.getData());
            String tool = extractTool(response);
            saveChatForAI(request, response,outputJson, (tool!=null || tool.equals("NONE")) ? -1 : null, tool);
        } catch (Exception e) {
            log.error("Failed to save chat async", e);
        }
    }

    @Async
    @Transactional
    public void saveForUserAsync(ChatBotRequest request) {
        try {
            saveChatForUser(request, 0);
        } catch (Exception e) {
            log.error("Failed to save chat async", e);
        }
    }

    public void saveChatForUser(ChatBotRequest chatBotRequest, Integer token) {
        ChatMessage chatMessage = ChatMessage.builder()
                .role(ChatRole.valueOf(chatBotRequest.getChatRole()))
                .inputImageUrl(chatBotRequest.getImageUrl())
                .inputText(chatBotRequest.getMessage())
                .chatTool(ChatTool.NONE)
                .tokenUsage(token)
                .userId(chatBotRequest.getUserId())
                .outputJson(null)
                .build();

        chatbotRepository.save(chatMessage);
    }

    private String extractTool(ChatbotResponse response) {
        if (response.getMeta() != null && response.getMeta().get("tool") != null) {
            return String.valueOf(response.getMeta().get("tool"));
        }
        return "NONE";
    }
}
