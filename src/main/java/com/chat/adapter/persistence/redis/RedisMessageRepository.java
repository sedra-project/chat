package com.chat.adapter.persistence.redis;

import com.chat.application.port.out.MessageRepositoryPort;
import com.chat.domain.model.ChatMessage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
@Profile("redis")
public class RedisMessageRepository implements MessageRepositoryPort {

    private final StringRedisTemplate redis;
    private final ObjectMapper mapper;
    private final String baseKey;
    private final int max;

    public RedisMessageRepository(StringRedisTemplate redis, ObjectMapper mapper,
                                  @Value("${chat.message.recent-size:100}") int max,
                                  @Value("${chat.message.redis-key:chat:messages}") String baseKey) {
        this.redis = redis;
        this.mapper = mapper;
        this.max = max;
        this.baseKey = baseKey;
    }

    // Utilise des clés par conversation pour plus de flexibilité
    private String resolveKey(String conversationId) {
        return baseKey + ":" + conversationId;
    }

    @Override
    public void append(ChatMessage message) {
        try {
            String json = mapper.writeValueAsString(message);
            String convKey = resolveKey(message.getConversationId());

            // On stocke dans la liste spécifique à la conversation
            redis.opsForList().leftPush(convKey, json);
            redis.opsForList().trim(convKey, 0, max - 1);

            // Optionnel : On stocke aussi dans une clé globale pour la compatibilité avec "recent"
            redis.opsForList().leftPush(baseKey + ":global", json);
            redis.opsForList().trim(baseKey + ":global", 0, max - 1);

        } catch (JsonProcessingException e) {
            throw new RuntimeException("Erreur de sérialisation JSON", e);
        }
    }

    @Override
    public List<ChatMessage> recent(int maxRequested) {
        return fetchFromRedis(baseKey + ":global", maxRequested);
    }

    @Override
    public Optional<ChatMessage> findById(String messageId) {
        // On cherche dans la liste globale car on ne connaît pas la conversationId à l'avance
        List<ChatMessage> globalMessages = recent(max);
        return globalMessages.stream()
                .filter(m -> m.getId() != null && m.getId().equals(messageId))
                .findFirst();
    }

    // Implémentation requise par l'interface pour l'historique
    public List<ChatMessage> findByConversationId(String conversationId) {
        return fetchFromRedis(resolveKey(conversationId), max);
    }

    //Implémentation cruciale pour valider les réactions
    @Override
    public boolean existsByIdAndConversationId(String messageId, String conversationId) {
        List<ChatMessage> messages = findByConversationId(conversationId);
        return messages.stream()
                .anyMatch(m -> m.getId() != null && m.getId().equals(messageId));
    }

    // Helper pour éviter la duplication de code
    private List<ChatMessage> fetchFromRedis(String redisKey, int count) {
        List<String> raw = redis.opsForList().range(redisKey, 0, count - 1);
        List<ChatMessage> out = new ArrayList<>();
        if (raw != null) {
            for (String s : raw) {
                try {
                    out.add(mapper.readValue(s, ChatMessage.class));
                } catch (IOException e) {
                    // Log ou ignore les messages malformés
                }
            }
        }
        return out;
    }
}