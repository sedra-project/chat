package com.chat.application.dto;

public class SessionResult {
    private final String token;
    private final String username;

    public SessionResult(String token, String username) {
        this.token = token;
        this.username = username;
    }
    public String getToken() { return token; }
    public String getUsername() { return username; }
}