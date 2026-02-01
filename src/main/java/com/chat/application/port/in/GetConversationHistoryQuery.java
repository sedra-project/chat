package com.chat.application.port.in;

import com.chat.domain.model.ChatMessage;

import java.util.List;

public interface GetConversationHistoryQuery {
    List<ChatMessage> history(String username, String conversationId, int max);
}