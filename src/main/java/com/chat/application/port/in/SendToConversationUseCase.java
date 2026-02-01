package com.chat.application.port.in;

import com.chat.domain.model.ChatMessage;
import com.chat.domain.model.ReplyTo;

public interface SendToConversationUseCase {
    ChatMessage send(String senderUserId, String conversationId, String content, ReplyTo replyTo);
}