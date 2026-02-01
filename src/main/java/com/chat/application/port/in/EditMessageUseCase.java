package com.chat.application.port.in;


import com.chat.domain.model.ChatMessage;

public interface EditMessageUseCase {
    ChatMessage edit(String conversationId, String messageId, String editorUserId, String newContent);
}
