package com.chat.adapter.web.dto;

public class SessionResponse {
    private final String token;
    private final String username;

    private String userId;

    public SessionResponse(String token, String username, String userId) {
        this.token = token;
        this.username = username;
        this.userId = userId;
    }

    public String getToken() {
        return token;
    }

    public String getUsername() {
        return username;
    }

    public String getUserId() {
        return userId;
    }
}
