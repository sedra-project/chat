package com.chat.application.port.out;

import com.chat.application.dto.ConversationSummary;

import java.util.List;

public interface ConversationRepositoryPort {
    String getOrCreateDirect(String userA, String userB);

    boolean isMember(String conversationId, String username);

    void addMember(String conversationId, String username); // utile pour GROUP plus tard

    List<String> membersOf(String conversationId);

    List<ConversationSummary> listForUser(String username);
}