package com.chat.adapter.web.dto;

public class SimpleMessageResponse {
    private final String message;

    public SimpleMessageResponse(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
