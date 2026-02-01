package com.chat.adapter.persistence.jpa.repository.messages;

import com.chat.adapter.persistence.jpa.entity.MessageEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MessageJpaRepository extends JpaRepository<MessageEntity, String> {
    // Récupérer les messages d'une conversation, triés du plus récent au plus vieux
    List<MessageEntity> findByConversationIdOrderByTimestampDesc(String conversationId, Pageable pageable);
}
