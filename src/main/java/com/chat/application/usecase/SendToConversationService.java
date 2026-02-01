package com.chat.application.usecase;

import com.chat.application.port.in.SendToConversationUseCase;
import com.chat.application.port.out.ConversationMessageRepositoryPort;
import com.chat.application.port.out.ConversationRepositoryPort;
import com.chat.application.port.out.RateLimiterPort;
import com.chat.application.port.out.UserRepositoryPort;
import com.chat.domain.exceptions.DomainException;
import com.chat.domain.model.ChatMessage;
import com.chat.domain.model.ReplyTo;
import com.chat.domain.model.User;
import com.chat.domain.service.ChatPolicy;
import org.springframework.stereotype.Service;

@Service
public class SendToConversationService implements SendToConversationUseCase {

    private final ConversationRepositoryPort convs;
    private final ConversationMessageRepositoryPort messages;
    private final ChatPolicy policy;
    private final RateLimiterPort limiter;
    private final UserRepositoryPort users;

    public SendToConversationService(ConversationRepositoryPort convs,
                                     ConversationMessageRepositoryPort messages,
                                     ChatPolicy policy,
                                     RateLimiterPort limiter,
                                     UserRepositoryPort users) {
        this.convs = convs;
        this.messages = messages;
        this.policy = policy;
        this.limiter = limiter;
        this.users = users;
    }

    @Override
    public ChatMessage send(String senderUserId, String conversationId, String content, ReplyTo replyTo) {

        User user = users.findById(senderUserId)
                .orElseThrow(() -> new DomainException("Utilisateur introuvable"));

        String senderUsername = user.getUsername().value();

        if (!convs.isMember(conversationId, senderUsername)) {
            throw new DomainException("Accès refusé à cette conversation");
        }

        String normalized = policy.normalizeAndValidate(content);

        if (!limiter.tryConsume(senderUserId)) {
            throw new DomainException("Trop de messages (anti-flood)");
        }

        ChatMessage message = ChatMessage.chatNew(
                conversationId,
                user.getId(),
                senderUsername,
                normalized,
                replyTo
        );

        messages.append(conversationId, message);
        return message;
    }
}