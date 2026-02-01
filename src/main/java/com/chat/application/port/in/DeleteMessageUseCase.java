package com.chat.application.port.in;


import com.chat.domain.model.ChatMessage;

public interface DeleteMessageUseCase {
    ChatMessage delete(String conversationId, String messageId, String requesterUserId);
}
