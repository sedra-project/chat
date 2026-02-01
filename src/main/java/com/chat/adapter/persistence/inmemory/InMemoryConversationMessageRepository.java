package com.chat.adapter.persistence.inmemory;

import com.chat.application.port.out.ConversationMessageRepositoryPort;
import com.chat.domain.model.ChatMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Profile("inmem")
public class InMemoryConversationMessageRepository implements ConversationMessageRepositoryPort {

    private final int max;
    private final Map<String, Deque<ChatMessage>> messagesByConv = new ConcurrentHashMap<>();

    public InMemoryConversationMessageRepository(@Value("${chat.message.recent-size:100}") int max) {
        this.max = Math.max(1, max);
    }

    @Override
    public void append(String conversationId, ChatMessage message) {
        Deque<ChatMessage> dq = messagesByConv.computeIfAbsent(conversationId, k -> new ArrayDeque<>());
        synchronized (dq) {
            dq.addFirst(message);
            while (dq.size() > max) dq.removeLast();
        }
    }

    @Override
    public List<ChatMessage> recent(String conversationId, int maxRequested) {
        Deque<ChatMessage> dq = messagesByConv.get(conversationId);
        if (dq == null) return Collections.emptyList();
        synchronized (dq) {
            int n = Math.min(Math.max(1, maxRequested), Math.min(max, dq.size()));
            List<ChatMessage> out = new ArrayList<>(n);
            int i = 0;
            for (ChatMessage m : dq) {
                if (i++ >= n) break;
                out.add(m);
            }
            return out;
        }
    }
    @Override
    public Optional<ChatMessage> findById(String conversationId, String messageId) {
        if (conversationId == null || messageId == null) return Optional.empty();

        Deque<ChatMessage> dq = messagesByConv.get(conversationId);
        if (dq == null) return Optional.empty();

        synchronized (dq) {
            return dq.stream()
                    .filter(m -> messageId.equals(m.getId()))
                    .findFirst();
        }
    }

    @Override
    public void update(ChatMessage message) {

        Deque<ChatMessage> dq = messagesByConv.get(message.getConversationId());
        if (dq != null) {
            synchronized (dq) {
                // Vu que c'est "In Memory" pour du dev, on peut laisser vide si car
                // UseCase modifie l'objet retourné par findById directement.
            }
        }

    }
}