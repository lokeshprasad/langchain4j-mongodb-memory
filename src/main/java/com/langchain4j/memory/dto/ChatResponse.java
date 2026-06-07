package com.langchain4j.memory.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.langchain4j.memory.model.Conversation;

public class ChatResponse {
    @JsonProperty("conversationId")
    private String conversationId;
    
    @JsonProperty("userMessage")
    private String userMessage;
    
    @JsonProperty("aiResponse")
    private String aiResponse;
    
    @JsonProperty("conversation")
    private Conversation conversation;

    public ChatResponse() {}

    public ChatResponse(String conversationId, String userMessage, String aiResponse, Conversation conversation) {
        this.conversationId = conversationId;
        this.userMessage = userMessage;
        this.aiResponse = aiResponse;
        this.conversation = conversation;
    }

    public String getConversationId() { return conversationId; }
    public void setConversationId(String conversationId) { this.conversationId = conversationId; }

    public String getUserMessage() { return userMessage; }
    public void setUserMessage(String userMessage) { this.userMessage = userMessage; }

    public String getAiResponse() { return aiResponse; }
    public void setAiResponse(String aiResponse) { this.aiResponse = aiResponse; }

    public Conversation getConversation() { return conversation; }
    public void setConversation(Conversation conversation) { this.conversation = conversation; }
}
