package com.chat.adapter.persistence.common;

import com.chat.application.port.out.RateLimiterPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimiter implements RateLimiterPort {

    private final int limitPerSecond;
    private final Map<String, Counter> counters = new ConcurrentHashMap<>();

    public RateLimiter(@Value("${chat.rate.limit-per-second:5}") int limitPerSecond) {
        this.limitPerSecond = limitPerSecond;
    }

    @Override
    public boolean tryConsume(String key) {
        long sec = System.currentTimeMillis() / 1000L;
        Counter counter = counters.computeIfAbsent(key, k -> new Counter());
        synchronized (counter) {
            if (counter.windowSec != sec) {
                counter.windowSec = sec;
                counter.count = 0;
            }
            counter.count++;
            return counter.count <= limitPerSecond;
        }
    }

    private static class Counter {
        long windowSec;
        int count;
    }
}