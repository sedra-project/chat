package com.chat.adapter.persistence.jpa.adapter.conversations;

import com.chat.adapter.persistence.jpa.entity.MessageEntity;
import com.chat.adapter.persistence.jpa.mapper.ChatMessageJpaMapper;
import com.chat.adapter.persistence.jpa.repository.messages.MessageJpaRepository;
import com.chat.application.port.out.ConversationMessageRepositoryPort;
import com.chat.domain.model.ChatMessage;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;


@Component
@Profile("inDB")
public class DbPublicMessageRepository implements ConversationMessageRepositoryPort {

    private final MessageJpaRepository jpaRepository;

    public DbPublicMessageRepository(MessageJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    @Transactional
    public void append(String conversationId, ChatMessage message) {
        MessageEntity entity = ChatMessageJpaMapper.toEntity(message);
        jpaRepository.save(entity);
    }

    @Override
    @Transactional
    public void update(ChatMessage message) {
        // JPA save fonctionne en "upsert" (update si l'ID existe)
        MessageEntity entity = ChatMessageJpaMapper.toEntity(message);
        jpaRepository.save(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChatMessage> recent(String conversationId, int maxRequested) {
        // On demande à la BD les X plus récents
        return jpaRepository.findByConversationIdOrderByTimestampDesc(conversationId, PageRequest.of(0, maxRequested))
                .stream()
                .map(ChatMessageJpaMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ChatMessage> findById(String conversationId, String messageId) {
        return jpaRepository.findById(messageId)
                // Sécurité supplémentaire : vérifier que le message appartient bien à la conversation
                .filter(entity -> entity.getConversationId().equals(conversationId))
                .map(ChatMessageJpaMapper::toDomain);
    }

}
