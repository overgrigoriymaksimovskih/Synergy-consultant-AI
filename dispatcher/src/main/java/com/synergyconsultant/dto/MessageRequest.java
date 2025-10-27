package com.synergyconsultant.dto;

public class MessageRequest {
    private String userId;
    private String message;

    // Геттеры и сеттеры (или используй Lombok @Data для упрощения)
    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}

