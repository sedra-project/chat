package com.chat.adapter.persistence.redis;

import com.chat.application.port.out.SessionTokenRepositoryPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
@Component
@Profile("redis")
public class RedisSessionTokenRepository implements SessionTokenRepositoryPort {

    private final StringRedisTemplate redis;
    private final long ttlMinutes;

    public RedisSessionTokenRepository(StringRedisTemplate redis,
                                       @Value("${chat.session.ttl-minutes:10}") long ttlMinutes) {
        this.redis = redis;
        this.ttlMinutes = ttlMinutes;
    }

    @Override
    public String createToken(String username) {
        String token = UUID.randomUUID().toString();
        String key = key(token);
        redis.opsForValue().set(key, username, ttlMinutes, TimeUnit.MINUTES);
        return token;
    }

    @Override
    public Optional<String> findUserIdByToken(String token) {
        String v = redis.opsForValue().get(key(token));
        return Optional.ofNullable(v);
    }

    @Override
    public void deleteToken(String token) {
        redis.delete(key(token));
    }

    private String key(String token) {
        return "chat:session:" + token;
    }
}