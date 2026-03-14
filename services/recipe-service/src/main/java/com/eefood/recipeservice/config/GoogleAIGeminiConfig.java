package com.eefood.recipeservice.config;

import com.eefood.recipeservice.service.nutrition.AIService;
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

    @Value("${google.ai.gemini.model}")
    private String geminiModel;

    @Value("${google.ai.gemini.temperature:0.1}")
    private Double geminiTemperature;

    @Bean
    public GoogleAiGeminiChatModel googleAiGeminiImageModel() {
        return GoogleAiGeminiChatModel.builder()
                .apiKey(geminiApiKey)
                .modelName(geminiModel)
                .temperature(geminiTemperature)
                .build();
    }

    @Bean
    public AIService chatbotAIService(
            GoogleAiGeminiChatModel geminiChatModel
    ) {
        return AiServices.builder(AIService.class)
                .chatModel(geminiChatModel)
                .build();
    }
}
