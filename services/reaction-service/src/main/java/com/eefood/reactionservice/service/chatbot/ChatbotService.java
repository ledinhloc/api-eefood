package com.eefood.reactionservice.service.chatbot;

import com.eefood.reactionservice.dto.request.ChatBotRequest;
import com.eefood.reactionservice.enums.ChatRole;
import com.eefood.reactionservice.enums.ChatTool;
import com.eefood.reactionservice.model.chatbot.ChatMessage;
import com.eefood.reactionservice.repository.chatbot.ChatbotRepository;
import com.eefood.reactionservice.util.ImageUtils;
import com.eefood.reactionservice.util.PromptLoader;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.data.image.Image;
import dev.langchain4j.data.message.Content;
import dev.langchain4j.data.message.ImageContent;
import dev.langchain4j.data.message.TextContent;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatbotService {
    private final ChatbotRepository chatbotRepository;
    private final GoogleAiGeminiChatModel geminiChatModel;
    private final ObjectMapper objectMapper;
    private final PromptLoader promptLoader;

    public String handleChat(ChatBotRequest chatBotRequest) {
        String systemPrompt = promptLoader.load("chatbot_system_prompt.txt");
        return "";
    }

    public void createChat(ChatBotRequest chatBotRequest, JsonNode output, Integer token) {
        ChatMessage chatMessage = ChatMessage.builder()
                .role(ChatRole.valueOf(chatBotRequest.getChatRole()))
                .inputImageUrl(chatBotRequest.getImageUrl())
                .inputText(chatBotRequest.getMessage())
                .chatTool(ChatTool.valueOf(chatBotRequest.getChatTool()))
                .tokenUsage(token)
                .userId(chatBotRequest.getUserId())
                .outputJson(output)
                .build();

        chatbotRepository.save(chatMessage);
    }

    public String callGemini(String prompt, String imageUrl) {
        List<Content> contents = new ArrayList<>();
        contents.add(TextContent.from(prompt));

        if (imageUrl != null && !imageUrl.isEmpty()) {
            contents.add(handleImageUrl(imageUrl));
        }

        ChatRequest chatRequest = ChatRequest.builder()
                .messages(UserMessage.from(contents))
                .build();

        ChatResponse response = geminiChatModel.chat(chatRequest);
        return response.aiMessage().text();
    }

    public ImageContent handleImageUrl(String imageUrl) {
        String base64Image = ImageUtils.downloadAndEncodeImage(imageUrl);
        if (base64Image != null) {
            String mimeType = ImageUtils.getMimeType(imageUrl);
            Image image = Image.builder()
                    .base64Data(base64Image)
                    .mimeType(mimeType)
                    .build();
            return  ImageContent.from(image);
        }
        return null;
    }
}
