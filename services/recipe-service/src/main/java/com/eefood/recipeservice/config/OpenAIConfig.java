package com.eefood.recipeservice.config;

import com.eefood.recipeservice.service.nutrition.AIService;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenAIConfig {
    @Value("${openai.api-key}")
    private String apiKey;

    @Value("${openai.model}")
    private String model;

    @Bean
    public OpenAiChatModel openAiChatModel() {
        return OpenAiChatModel.builder()
                .apiKey(apiKey)
                .modelName(model)
                .maxRetries(3)
                .build();
    }

    @Bean
    public AIService chatbotAIService(
            OpenAiChatModel openAiChatModel
    ) {
        return AiServices.builder(AIService.class)
                .chatModel(openAiChatModel)
                .build();
    }
}
