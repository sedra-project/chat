package com.chat.application.dto;

import com.chat.domain.model.ChatMessage;

import java.util.List;

public class InitialState {
    private final List<String> users;
    private final List<ChatMessage> messages;

    public InitialState(List<String> users, List<ChatMessage> messages) {
        this.users = users; this.messages = messages;
    }
    public List<String> getUsers() { return users; }
    public List<ChatMessage> getMessages() { return messages; }
}