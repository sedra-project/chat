package com.chat.application.port.in;

import com.chat.application.dto.ConversationSummary;

public interface StartDirectConversationUseCase {
    ConversationSummary start(String requester, String peer);
}