package com.chat.application.usecase;

import com.chat.application.port.in.GetConversationHistoryQuery;
import com.chat.application.port.out.ConversationMessageRepositoryPort;
import com.chat.application.port.out.ConversationRepositoryPort;
import com.chat.domain.exceptions.DomainException;
import com.chat.domain.model.ChatMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GetConversationHistoryService implements GetConversationHistoryQuery {
    private final ConversationRepositoryPort convs;
    private final ConversationMessageRepositoryPort messages;
    private final int defaultMax;

    public GetConversationHistoryService(ConversationRepositoryPort convs,
                                         ConversationMessageRepositoryPort messages,
                                         @Value("${chat.message.recent-size:100}") int defaultMax) {
        this.convs = convs;
        this.messages = messages;
        this.defaultMax = defaultMax;
    }

    @Override
    public List<ChatMessage> history(String username, String conversationId, int max) {
        if (!convs.isMember(conversationId, username)) {
            throw new DomainException("Accès refusé!!!");
        }
        int number = max > 0 ? Math.min(max, defaultMax) : defaultMax;
        return messages.recent(conversationId, number);
    }
}