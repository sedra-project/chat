package com.chat.application.port.out;

import com.chat.domain.model.ChatMessage;

public interface MessageEventPublisherPort {
    void messageUpdated(ChatMessage message);

    void messageDeleted(ChatMessage message);
}
