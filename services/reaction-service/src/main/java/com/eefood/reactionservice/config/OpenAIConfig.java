package com.eefood.reactionservice.config;

import dev.langchain4j.model.openai.OpenAiChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenAIConfig {

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
}
