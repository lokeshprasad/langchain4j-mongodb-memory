package com.langchain4j.memory.model;

import java.util.List;

public class Conversation {
    private String id;
    private List<Message> messages;

    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public List<Message> getMessages() { return messages; }
    public void setMessages(List<Message> messages) { this.messages = messages; }
}