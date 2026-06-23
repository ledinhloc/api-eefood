package com.eefood.reactionservice.config;

import com.eefood.reactionservice.service.chatbot.ChatbotAIService;
import com.eefood.reactionservice.service.chatbot.tools.ChatbotTools;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.googleai.GoogleAiEmbeddingModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiStreamingChatModel;
import dev.langchain4j.service.AiServices;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GoogleAIGeminiConfig {
    @Value("${google.ai.gemini.api-key}")
    private String geminiApiKey;

    @Value("${google.ai.gemini.model:gemini-2.5-pro}")
    private String geminiModel;

    @Value("${google.ai.gemini.temperature:0.1}")
    private Double geminiTemperature;

    @Value("${google.ai.gemini.embedding-model:text-embedding-004}")
    private String embeddingModel;

    @Bean
    public GoogleAiGeminiStreamingChatModel geminiStreamingChatModel() {
        return GoogleAiGeminiStreamingChatModel.builder()
                .apiKey(geminiApiKey)
                .modelName(geminiModel)
                .temperature(geminiTemperature)
                .build();
    }

    @Bean
    public GoogleAiGeminiChatModel googleAiGeminiImageModel() {
        return GoogleAiGeminiChatModel.builder()
                .apiKey(geminiApiKey)
                .modelName(geminiModel)
                .temperature(geminiTemperature)
                .maxRetries(3)
                .build();
    }

    @Bean
    public ChatbotAIService chatbotAIService(
            GoogleAiGeminiChatModel geminiChatModel,
            GoogleAiGeminiStreamingChatModel geminiStreamingChatModel,
            ChatbotTools chatbotTools
    ) {
        return AiServices.builder(ChatbotAIService.class)
                .chatModel(geminiChatModel)
                .streamingChatModel(geminiStreamingChatModel)
                .tools(chatbotTools)
                .build();
    }

    @Bean
    public EmbeddingModel embeddingModel() {
        return GoogleAiEmbeddingModel.builder()
                .apiKey(geminiApiKey)
                .modelName(embeddingModel)
                .build();
    }
}
