package com.chat.application.usecase;

import com.chat.adapter.persistence.jpa.entity.MessageReactionEntity;
import com.chat.adapter.persistence.jpa.repository.reactions.MessageReactionJpaRepository;
import com.chat.application.dto.ReactionEventDTO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReactionService {

    private final MessageReactionJpaRepository repo;

    public ReactionService(MessageReactionJpaRepository repo) {
        this.repo = repo;
    }

    @Transactional
    public ReactionEventDTO toggle(String messageId, String emoji, String username) {
        boolean exists = repo.existsByMessageIdAndUsernameAndEmoji(messageId, username, emoji);

        if (exists) {
            repo.deleteByMessageIdAndUsernameAndEmoji(messageId, username, emoji);
            return new ReactionEventDTO(messageId, emoji, -1);
        } else {
            MessageReactionEntity messageReactionEntity = new MessageReactionEntity();
            messageReactionEntity.setMessageId(messageId);
            messageReactionEntity.setUsername(username);
            messageReactionEntity.setEmoji(emoji);
            repo.save(messageReactionEntity);
            return new ReactionEventDTO(messageId, emoji, +1);
        }
    }
}
