package com.chat.adapter.persistence.redis;

import com.chat.application.port.out.UsernameReservationPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
@Profile("redis")
public class RedisUsernameReservation implements UsernameReservationPort {

    private final StringRedisTemplate redis;
    private final long ttlSeconds;

    public RedisUsernameReservation(StringRedisTemplate redis,
                                    @Value("${chat.username.reserve-ttl-seconds:60}") long ttlSeconds) {
        this.redis = redis;
        this.ttlSeconds = ttlSeconds;
    }

    @Override
    public boolean tryReserve(String username) {
        String key = key(username);
        Boolean created = redis.opsForValue().setIfAbsent(key, "1");
        if (Boolean.TRUE.equals(created)) {
            redis.expire(key, ttlSeconds, TimeUnit.SECONDS); // TTL pour éviter les réservations orphelines
            return true;
        }
        return false; // déjà réservé
    }

    @Override
    public void release(String username) {
        redis.delete(key(username));
    }

    private String key(String username) { return "chat:username:resv:" + username; }
}