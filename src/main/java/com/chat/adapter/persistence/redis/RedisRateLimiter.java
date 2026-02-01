package com.chat.adapter.persistence.redis;

import com.chat.application.port.out.RateLimiterPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
@Profile("redis")
public class RedisRateLimiter implements RateLimiterPort {

    private final StringRedisTemplate redis;
    private final int limitPerSecond;

    public RedisRateLimiter(StringRedisTemplate redis,
                            @Value("${chat.rate.limit-per-second:5}") int limitPerSecond) {
        this.redis = redis; this.limitPerSecond = limitPerSecond;
    }

    @Override
    public boolean tryConsume(String username) {
        long sec = System.currentTimeMillis() / 1000L;
        String key = "chat:rate:" + username + ":" + sec;
        Long count = redis.opsForValue().increment(key, 1L);
        if (count != null && count == 1L) {
            redis.expire(key, 2, TimeUnit.SECONDS); // 1s + marge
        }
        return count != null && count <= limitPerSecond;
    }
}