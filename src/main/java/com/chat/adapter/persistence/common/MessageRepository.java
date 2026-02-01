package com.chat.adapter.persistence.common;

import com.chat.application.port.out.MessageRepositoryPort;
import com.chat.domain.model.ChatMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class MessageRepository implements MessageRepositoryPort {

    private final int max;
    private static final Deque<ChatMessage> deque = new ArrayDeque<>();
    private static final Object lock = new Object();

    public MessageRepository(@Value("${chat.message.recent-size:100}") int max) {
        this.max = Math.max(10, max);
    }

    @Override
    public void append(ChatMessage message) {
        synchronized (lock) {
            deque.addFirst(message);
            if (deque.size() > max) {
                deque.removeLast();
            }
        }
    }

    @Override
    public List<ChatMessage> recent(int maxRequested) {
        synchronized (lock) {
            int n = Math.min(maxRequested, deque.size());
            List<ChatMessage> out = new ArrayList<>(n);
            int i = 0;
            for (ChatMessage m : deque) {
                if (i++ >= n) break;
                out.add(m);
            }
            return out;
        }
    }

    @Override
    public Optional<ChatMessage> findById(String messageId) {
        if (messageId == null) return Optional.empty();

        synchronized (lock) {
            Optional<ChatMessage> found = deque.stream()
                    .filter(m -> messageId.trim().equals(m.getId()))
                    .findFirst();

            if (!found.isPresent()) {
                // DEBUG : Permet de voir si le message a disparu ou si l'ID est incorrect
                System.out.println("[DEBUG] findById échoué pour ID: " + messageId);
                System.out.println("[DEBUG] Messages en mémoire: " + deque.size());
            }

            return found;
        }
    }

    @Override
    public boolean existsByIdAndConversationId(String messageId, String conversationId) {
        if (messageId == null || conversationId == null) return false;

        synchronized (lock) {
            return deque.stream()
                    .anyMatch(m -> messageId.trim().equals(m.getId()) &&
                            conversationId.equals(m.getConversationId()));
        }
    }
}