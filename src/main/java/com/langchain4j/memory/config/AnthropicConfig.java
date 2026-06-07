package com.langchain4j.memory.config;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.anthropic.AnthropicChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

@Configuration
@ConditionalOnProperty(name = "llm.provider", havingValue = "anthropic")
public class AnthropicConfig {

    @Value("${anthropic.api.key}")
    private String anthropicApiKey;

    @Value("${anthropic.model:claude-3-5-sonnet-20241022}")
    private String modelName;

    @Bean
    public ChatLanguageModel chatLanguageModel() {
        return AnthropicChatModel.builder()
                .apiKey(anthropicApiKey)
                .modelName(modelName)
                .maxTokens(1024)
                .build();
    }
}
