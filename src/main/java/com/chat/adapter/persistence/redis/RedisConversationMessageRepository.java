package com.chat.adapter.persistence.redis;

import com.chat.application.port.out.ConversationMessageRepositoryPort;
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
public class RedisConversationMessageRepository implements ConversationMessageRepositoryPort {

    private final StringRedisTemplate redis;
    private final ObjectMapper mapper;
    private final int max;

    public RedisConversationMessageRepository(StringRedisTemplate redis,
                                              ObjectMapper mapper,
                                              @Value("${chat.message.recent-size:100}") int max) {
        this.redis = redis;
        this.mapper = mapper;
        this.max = Math.max(1, max);
    }

    @Override
    public void append(String conversationId, ChatMessage message) {
        String key = messagesKey(conversationId);
        try {
            String json = mapper.writeValueAsString(message);
            redis.opsForList().leftPush(key, json);
            redis.opsForList().trim(key, 0, max - 1);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Unable to serialize ChatMessage", e);
        }
    }

    @Override
    public List<ChatMessage> recent(String conversationId, int maxRequested) {
        String key = messagesKey(conversationId);
        int n = Math.min(Math.max(1, maxRequested), max);
        List<String> raw = redis.opsForList().range(key, 0, n - 1);
        List<ChatMessage> out = new ArrayList<>();
        if (raw == null) return out;
        for (String s : raw) {
            try {
                out.add(mapper.readValue(s, ChatMessage.class));
            } catch (IOException ignored) {
                // ignorer les messages corrompus
            }
        }
        return out;
    }

    @Override
    public Optional<ChatMessage> findById(String conversationId, String messageId) {
        if (conversationId == null || messageId == null) return Optional.empty();

        List<ChatMessage> messages = recent(conversationId, max);

        return messages.stream()
                .filter(m -> messageId.equals(m.getId()))
                .findFirst();
    }

    @Override
    public void update(ChatMessage message) {
        String key = messagesKey(message.getConversationId());

        // 1. On récupère tout (dans la limite du max stocké)
        List<String> rawList = redis.opsForList().range(key, 0, max - 1);
        if (rawList == null || rawList.isEmpty()) return;

        try {
            // 2. On cherche l'index du message à modifier
            for (int i = 0; i < rawList.size(); i++) {
                String json = rawList.get(i);
                // Optimisation : on vérifie si le JSON contient l'ID avant de parser
                if (json.contains(message.getId())) {
                    ChatMessage candidate = mapper.readValue(json, ChatMessage.class);
                    if (candidate.getId().equals(message.getId())) {

                        // 3. Si trouvé -> On écrase avec la nouvelle version (qui contient les réactions)
                        String updatedJson = mapper.writeValueAsString(message);
                        redis.opsForList().set(key, i, updatedJson);
                        return; // Fin
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Erreur lors de la mise à jour Redis", e);
        }
    }

    private String messagesKey(String convId) {
        return "conv:" + convId + ":messages";
    }
}