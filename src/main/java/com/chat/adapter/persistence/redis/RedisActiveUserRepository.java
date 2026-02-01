package com.chat.adapter.persistence.redis;

import com.chat.application.port.out.ActiveUserRepositoryPort;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Component
@Profile("redis")
public class RedisActiveUserRepository implements ActiveUserRepositoryPort {

    private final StringRedisTemplate redis;
    private static final String KEY = "chat:active-users";

    public RedisActiveUserRepository(StringRedisTemplate redis) { this.redis = redis; }

    @Override public boolean add(String username) { return redis.opsForSet().add(KEY, username) == 1L; }
    @Override public boolean remove(String username) { return redis.opsForSet().remove(KEY, username) == 1L; }
    @Override public List<String> list() {
        Set<String> members = redis.opsForSet().members(KEY);
        return members == null ? new ArrayList<>() : new ArrayList<>(members);
    }
    @Override public boolean contains(String username) { return Boolean.TRUE.equals(redis.opsForSet().isMember(KEY, username)); }
}