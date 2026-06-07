package com.langchain4j.memory.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Document(collection = "conversations")
public class Conversation {
    @Id
    @JsonProperty("id")
    private String id;
    
    @JsonProperty("messages")
    private List<Message> messages;
    
    @JsonProperty("createdAt")
    private long createdAt;

    public Conversation() {
        this.id = UUID.randomUUID().toString();
        this.messages = new ArrayList<>();
        this.createdAt = System.currentTimeMillis();
    }

    public Conversation(String id) {
        this.id = id;
        this.messages = new ArrayList<>();
        this.createdAt = System.currentTimeMillis();
    }

    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public List<Message> getMessages() { return messages; }
    public void setMessages(List<Message> messages) { this.messages = messages; }
    
    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
    
    public void addMessage(Message message) {
        this.messages.add(message);
    }
}