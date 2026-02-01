package com.chat.application.port.out;

import com.chat.domain.model.ChatMessage;

import java.util.List;
import java.util.Optional;

public interface MessageRepositoryPort {
    void append(ChatMessage message);

    List<ChatMessage> recent(int max);

    Optional<ChatMessage> findById(String messageId);

    boolean existsByIdAndConversationId(String messageId, String conversationId);
}