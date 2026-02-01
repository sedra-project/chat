package com.chat.adapter.persistence.inmemory;

import com.chat.application.port.out.SessionTokenRepositoryPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Profile("inmem")
public class InMemorySessionTokenRepository implements SessionTokenRepositoryPort {

    private static class Entry {
        final String username;
        final long expiresAt;

        Entry(String username, long expiresAt) {
            this.username = username;
            this.expiresAt = expiresAt;
        }
    }

    private final long ttlMillis;
    private final Map<String, Entry> tokens = new ConcurrentHashMap<>();

    public InMemorySessionTokenRepository(@Value("${chat.session.ttl-minutes:10}") long ttlMinutes) {
        this.ttlMillis = ttlMinutes * 60_000L;
    }

    @Override
    public String createToken(String username) {
        String token = UUID.randomUUID().toString();
        long exp = System.currentTimeMillis() + ttlMillis;
        tokens.put(token, new Entry(username, exp));
        return token;
    }

    @Override
    public Optional<String> findUserIdByToken(String token) {
        Entry e = tokens.get(token);
        long now = System.currentTimeMillis();
        if (e == null) return Optional.empty();
        if (e.expiresAt < now) {
            tokens.remove(token);
            return Optional.empty();
        }
        return Optional.of(e.username);
    }

    @Override
    public void deleteToken(String token) {
        tokens.remove(token);
    }
}