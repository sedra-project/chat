package com.chat.application.usecase;

import com.chat.application.port.in.DeleteMessageUseCase;
import com.chat.application.port.out.ConversationMessageRepositoryPort;
import com.chat.application.port.out.MessageEventPublisherPort;
import com.chat.application.port.out.MessageRepositoryPort;
import com.chat.domain.exceptions.DomainException;
import com.chat.domain.model.ChatMessage;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DeleteMessageService implements DeleteMessageUseCase {

    private final MessageRepositoryPort publicRepo;
    private final ConversationMessageRepositoryPort convRepo;
    private final MessageEventPublisherPort events;

    public DeleteMessageService(MessageRepositoryPort publicRepo,
                                ConversationMessageRepositoryPort convRepo,
                                MessageEventPublisherPort events) {
        this.publicRepo = publicRepo;
        this.convRepo = convRepo;
        this.events = events;
    }

    @Override
    @Transactional
    public ChatMessage delete(String conversationId, String messageId, String requesterUserId) {
        ChatMessage msg = load(conversationId, messageId);

        msg.delete(requesterUserId);

        ChatMessage saved = save(conversationId, msg);

        events.messageDeleted(saved);
        return saved;
    }

    private ChatMessage load(String conversationId, String messageId) {
        if ("public".equals(conversationId)) {
            return publicRepo.findById(messageId)
                    .orElseThrow(() -> new DomainException("Message introuvable"));
        }
        return convRepo.findById(conversationId, messageId)
                .orElseThrow(() -> new DomainException("Message introuvable"));
    }

    private ChatMessage save(String conversationId, ChatMessage msg) {
        if ("public".equals(conversationId)) {
            publicRepo.append(msg);
            return msg;
        }
        convRepo.update(msg);
        return msg;
    }
}