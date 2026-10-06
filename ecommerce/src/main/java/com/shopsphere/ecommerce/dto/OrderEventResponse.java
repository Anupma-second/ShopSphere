package com.shopsphere.ecommerce.dto;

import java.time.LocalDateTime;

public class OrderEventResponse {

    private String type;
    private String message;
    private LocalDateTime createdAt;

    public OrderEventResponse() {
    }

    public OrderEventResponse(String type, String message, LocalDateTime createdAt) {
        this.type = type;
        this.message = message;
        this.createdAt = createdAt;
    }

    public String getType() { return type; }
    public String getMessage() { return message; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
