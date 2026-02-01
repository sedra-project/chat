package com.chat.application.usecase;

import com.chat.application.port.in.EditMessageUseCase;
import com.chat.application.port.out.ConversationMessageRepositoryPort;
import com.chat.application.port.out.MessageEventPublisherPort;
import com.chat.application.port.out.MessageRepositoryPort;
import com.chat.domain.exceptions.DomainException;
import com.chat.domain.model.ChatMessage;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EditMessageService implements EditMessageUseCase {

    private final MessageRepositoryPort publicRepo;
    private final ConversationMessageRepositoryPort convRepo;
    private final MessageEventPublisherPort events;

    public EditMessageService(MessageRepositoryPort publicRepo,
                              ConversationMessageRepositoryPort convRepo,
                              MessageEventPublisherPort events) {
        this.publicRepo = publicRepo;
        this.convRepo = convRepo;
        this.events = events;
    }

    @Override
    @Transactional
    public ChatMessage edit(String conversationId, String messageId, String editorUserId, String newContent) {
        ChatMessage msg = load(conversationId, messageId);

        msg.edit(editorUserId, newContent);

        ChatMessage saved = save(conversationId, msg);

        events.messageUpdated(saved);
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