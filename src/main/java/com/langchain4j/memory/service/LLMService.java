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

import java.util.ArrayList;
import java.util.List;

@Service
public class LLMService {

    private final ChatLanguageModel chatLanguageModel;

    @Autowired
    public LLMService(ChatLanguageModel chatLanguageModel) {
        this.chatLanguageModel = chatLanguageModel;
    }

    /**
     * Generate an AI response based on conversation history
     */
    public String generateResponse(Conversation conversation) {
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

        return response.content().text();
    }

    /**
     * Generate a response to a specific user message
     */
    public String generateResponseToMessage(String userMessage) {
        List<ChatMessage> messages = new ArrayList<>();
        messages.add(new SystemMessage("You are a helpful AI assistant. Provide clear, concise, and helpful responses."));
        messages.add(new UserMessage(userMessage));

        dev.langchain4j.model.output.Response<AiMessage> response = 
            chatLanguageModel.generate(messages);

        return response.content().text();
    }
}
