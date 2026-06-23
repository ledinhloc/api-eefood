package com.eefood.reactionservice.config;

import com.eefood.reactionservice.service.chatbot.ChatbotAIService;
import com.eefood.reactionservice.service.chatbot.tools.ChatbotTools;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import dev.langchain4j.service.AiServices;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenAIConfig {

    @Bean
    public EmbeddingModel embeddingModel(
            @Value("${openai.api-key}") String apiKey,
            @Value("${openai.embedding-model}") String model
    ) {
        return OpenAiEmbeddingModel.builder()
                .apiKey(apiKey)
                .modelName(model)
                .maxRetries(3)
                .build();
    }

    @Bean
    public OpenAiChatModel openAiModel(
            @Value("${openai.api-key}") String apiKey,
            @Value("${openai.meal-plan-model}") String model
    ) {
        return OpenAiChatModel.builder()
                .apiKey(apiKey)
                .modelName(model)
                .maxRetries(3)
                .build();
    }

    @Bean
    public OpenAiStreamingChatModel openAiStreamingChatModel(
            @Value("${openai.api-key}") String apiKey,
            @Value("${openai.meal-plan-model}") String model
    ) {
        return OpenAiStreamingChatModel.builder()
                .apiKey(apiKey)
                .modelName(model)
                .build();
    }

    @Bean
    public ChatbotAIService chatbotAIService(
            OpenAiChatModel openAiModel,
            OpenAiStreamingChatModel openAiStreamingChatModel,
            ChatbotTools chatbotTools
    ) {
        return AiServices.builder(ChatbotAIService.class)
                .chatModel(openAiModel)
                .streamingChatModel(openAiStreamingChatModel)
                .tools(chatbotTools)
                .build();
    }
}
