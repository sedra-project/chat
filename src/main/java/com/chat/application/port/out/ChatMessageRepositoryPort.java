package com.chat.application.port.out;

import com.chat.domain.model.ChatMessage;

import java.util.Optional;

public interface ChatMessageRepositoryPort {
    Optional<ChatMessage> findPublicById(String messageId);

    Optional<ChatMessage> findInConversationById(String conversationId, String messageId);

    ChatMessage savePublic(ChatMessage message);

    ChatMessage saveInConversation(String conversationId, ChatMessage message);
}
