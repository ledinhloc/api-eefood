package com.eefood.reactionservice.service.chatbot;

import com.eefood.reactionservice.dto.request.ChatBotRequest;
import com.eefood.reactionservice.dto.response.chatbot.ChatbotResponse;
import com.eefood.reactionservice.enums.ChatRole;
import com.eefood.reactionservice.enums.ChatTool;
import com.eefood.reactionservice.mapper.ChatbotMessageMapper;
import com.eefood.reactionservice.model.chatbot.ChatbotMessage;
import com.eefood.reactionservice.repository.chatbot.ChatbotRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatbotCrudService {
    private final ObjectMapper objectMapper;
    private final ChatbotRepository chatbotRepository;
    private final ChatbotMessageMapper chatbotMessageMapper;

    public List<ChatbotResponse> getListChatbotHistory(Long userId) {
        List<ChatbotMessage> listChatbotHistory = chatbotRepository.findAllByUserIdAndIsDeletedFalse(userId);
        List<ChatbotResponse> responses = listChatbotHistory.stream()
                .map(chatbotMessageMapper::toResponse)
                .toList();
        return responses;
    }

    @Transactional
    public void saveChatForAI(ChatBotRequest chatBotRequest, ChatbotResponse chatbotResponse, Integer token, String tool) {
        ChatbotMessage chatMessage = ChatbotMessage.builder()
                .role(ChatRole.AI)
                .imageUrl("")
                .message(chatbotResponse.getMessage())
                .chatTool(ChatTool.valueOf(tool))
                .tokenUsage(token)
                .userId(chatBotRequest.getUserId())
                .data(objectMapper.valueToTree(chatbotResponse.getData()))
                .meta(objectMapper.valueToTree(chatbotResponse.getMeta()))
                .build();

        chatbotRepository.save(chatMessage);
    }

    @Async
    @Transactional
    public void saveForAIAsync(ChatBotRequest request, ChatbotResponse response) {
        try {
            String tool = extractTool(response);
            saveChatForAI(request, response, (tool!=null || tool.equals("NONE")) ? -1 : null, tool);
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
        ChatbotMessage chatMessage = ChatbotMessage.builder()
                .role(ChatRole.valueOf(chatBotRequest.getChatRole()))
                .imageUrl(chatBotRequest.getImageUrl())
                .message(chatBotRequest.getMessage())
                .chatTool(ChatTool.NONE)
                .tokenUsage(token)
                .userId(chatBotRequest.getUserId())
                .data(null)
                .meta(null)
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
