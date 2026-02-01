package com.chat.application.usecase;

import com.chat.application.dto.ReactionToggleResult;
import com.chat.application.port.in.ToggleReactionUseCase;
import com.chat.application.port.out.ConversationMessageRepositoryPort;
import com.chat.application.port.out.MessageRepositoryPort;
import com.chat.application.port.out.UserRepositoryPort;
import com.chat.domain.model.ChatMessage;
import com.chat.domain.model.Reaction;
import org.springframework.messaging.MessagingException;
import org.springframework.stereotype.Service;

import java.util.Iterator;
import java.util.List;
import java.util.UUID;

@Service
public class AddReactionService implements ToggleReactionUseCase {

    private final ConversationMessageRepositoryPort conversationMessageRepositoryPort;
    private final MessageRepositoryPort messageRepository;
    private final UserRepositoryPort users;

    public AddReactionService(ConversationMessageRepositoryPort conversationMessageRepositoryPort,
                              MessageRepositoryPort messageRepository,
                              UserRepositoryPort users) {
        this.messageRepository = messageRepository;
        this.conversationMessageRepositoryPort = conversationMessageRepositoryPort;
        this.users = users;
    }

    @Override
    public ReactionToggleResult execute(String userId, String conversationId, String messageId, String emoji) {
        ChatMessage chatMessage = loadMessage(conversationId, messageId);

        boolean removed = removeIfExists(chatMessage.getReactions(), userId, emoji);

        if (removed) {
            persist(conversationId, chatMessage);
            return new ReactionToggleResult(chatMessage.getId(), emoji, -1);
        }

        String username = users.findById(userId)
                .map(u -> u.getUsername().value())
                .orElseThrow(() -> new MessagingException("Unauthorized"));

        Reaction reaction = new Reaction(
                UUID.randomUUID().toString(),
                chatMessage.getId(),
                userId,
                username,              // snapshot d’affichage
                emoji,
                System.currentTimeMillis()
        );

        chatMessage.addReaction(reaction);

        persist(conversationId, chatMessage);
        return new ReactionToggleResult(chatMessage.getId(), emoji, +1);
    }

    private ChatMessage loadMessage(String conversationId, String messageId) {
        if ("public".equals(conversationId)) {
            return messageRepository.findById(messageId)
                    .orElseThrow(() -> new MessagingException("Message introuvable"));
        }
        return conversationMessageRepositoryPort.findById(conversationId, messageId)
                .orElseThrow(() -> new MessagingException("Conversation introuvable"));
    }

    private void persist(String conversationId, ChatMessage chatMessage) {
        if ("public".equals(conversationId)) {
            messageRepository.append(chatMessage);
        } else {
            conversationMessageRepositoryPort.update(chatMessage);
        }
    }
    private boolean removeIfExists(List<Reaction> reactions, String userId, String emoji) {
        if (reactions == null || reactions.isEmpty()) return false;

        for (Iterator<Reaction> it = reactions.iterator(); it.hasNext(); ) {
            Reaction r = it.next();

            boolean sameUser = userId != null && userId.equals(r.getUserId());
            if (sameUser && emoji.equals(r.getEmoji())) {
                it.remove();
                return true;
            }
        }
        return false;
    }
}