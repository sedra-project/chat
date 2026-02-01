package com.chat.adapter.persistence.jpa.repository.reactions;

import com.chat.adapter.persistence.jpa.entity.MessageReactionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MessageReactionJpaRepository extends JpaRepository<MessageReactionEntity, Long> {
    boolean existsByMessageIdAndUsernameAndEmoji(String messageId, String username, String emoji);

    void deleteByMessageIdAndUsernameAndEmoji(String messageId, String username, String emoji);
}
