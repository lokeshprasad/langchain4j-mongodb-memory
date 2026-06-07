package com.langchain4j.memory.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ChatRequest {
    @JsonProperty("content")
    private String content;
    
    @JsonProperty("sender")
    private String sender;

    public ChatRequest() {}

    public ChatRequest(String content, String sender) {
        this.content = content;
        this.sender = sender;
    }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getSender() { return sender; }
    public void setSender(String sender) { this.sender = sender; }
}
