package com.synergyconsultant.entity;

public class ChatMessage {

    private long timestamp;
    private String role;  // "user" или "assistant"
    private String content;

    // Конструкторы, геттеры, сеттеры
    public ChatMessage() {}

    public ChatMessage(String role, String content) {
        this.timestamp = System.currentTimeMillis();
        this.role = role;
        this.content = content;
    }

    // Геттеры и сеттеры...
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
}
