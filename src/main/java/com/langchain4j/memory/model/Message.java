package com.langchain4j.memory.model;

public class Message {
    private String content;
    private String sender;
    private long timestamp;

    // Getters and setters
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getSender() { return sender; }
    public void setSender(String sender) { this.sender = sender; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}