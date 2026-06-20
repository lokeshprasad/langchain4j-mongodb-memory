package com.langchain4j.memory.service;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.AiMessage;
import com.langchain4j.memory.model.Conversation;
import com.langchain4j.memory.model.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

@Service
public class LLMService {

    private final ChatLanguageModel chatLanguageModel;
    private static final Logger log = LoggerFactory.getLogger(LLMService.class);

    @Autowired
    public LLMService(ChatLanguageModel chatLanguageModel) {
        this.chatLanguageModel = chatLanguageModel;
    }

    /**
     * Generate an AI response based on conversation history
     */
    public String generateResponse(Conversation conversation) {
        log.debug("LLMService.generateResponse() called for conversation id={}", conversation.getId());
        // Build chat messages from conversation history
        List<ChatMessage> chatMessages = new ArrayList<>();

        // Add system message
        chatMessages.add(new SystemMessage("You are a helpful AI assistant. Provide clear, concise, and helpful responses."));

        // Add conversation history
        for (Message msg : conversation.getMessages()) {
            if ("user".equalsIgnoreCase(msg.getSender())) {
                chatMessages.add(new UserMessage(msg.getContent()));
            } else if ("assistant".equalsIgnoreCase(msg.getSender())) {
                chatMessages.add(new AiMessage(msg.getContent()));
            }
        }

        // Get response from LLM
        dev.langchain4j.model.output.Response<AiMessage> response = 
            chatLanguageModel.generate(chatMessages);

        String text = response.content().text();
        log.debug("LLMService.generateResponse() returned {} chars for conversation id={}", text == null ? 0 : text.length(), conversation.getId());
        return text;
    }

    /**
     * Generate a response to a specific user message
     */
    public String generateResponseToMessage(String userMessage) {
        log.debug("LLMService.generateResponseToMessage() called");
        List<ChatMessage> messages = new ArrayList<>();
        messages.add(new SystemMessage("You are a helpful AI assistant. Provide clear, concise, and helpful responses."));
        messages.add(new UserMessage(userMessage));

        dev.langchain4j.model.output.Response<AiMessage> response = 
            chatLanguageModel.generate(messages);

        String text = response.content().text();
        log.debug("LLMService.generateResponseToMessage() returned {} chars", text == null ? 0 : text.length());
        return text;
    }
}
