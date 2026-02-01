package com.chat.application.usecase;

import com.chat.application.port.in.SendMessageUseCase;
import com.chat.application.port.out.MessageRepositoryPort;
import com.chat.application.port.out.RateLimiterPort;
import com.chat.application.port.out.UserRepositoryPort;
import com.chat.domain.exceptions.DomainException;
import com.chat.domain.model.ChatMessage;
import com.chat.domain.model.ReplyTo;
import com.chat.domain.model.User;
import com.chat.domain.service.ChatPolicy;
import org.springframework.stereotype.Service;

@Service
public class SendMessageService implements SendMessageUseCase {

    private final ChatPolicy policy;
    private final RateLimiterPort limiter;
    private final MessageRepositoryPort messages;
    private final UserRepositoryPort users;

    public SendMessageService(ChatPolicy policy,
                              RateLimiterPort limiter,
                              MessageRepositoryPort messages,
                              UserRepositoryPort users) {
        this.policy = policy;
        this.limiter = limiter;
        this.messages = messages;
        this.users = users;
    }

    @Override
    public ChatMessage send(String senderUserId, String conversationId, String content, ReplyTo replyTo) {
        String normalized = policy.normalizeAndValidate(content);

        if (!limiter.tryConsume(senderUserId)) {
            throw new DomainException("Trop de messages (anti-flood)");
        }

        User user = users.findById(senderUserId)
                .orElseThrow(() -> new DomainException("Utilisateur introuvable"));

        ChatMessage msg = ChatMessage.chatNew(
                conversationId,
                user.getId(),
                user.getUsername().value(),
                normalized,
                replyTo
        );

        messages.append(msg);
        return msg;
    }
}