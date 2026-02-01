package com.chat.application.port.out;

public interface RateLimiterPort {
    boolean tryConsume(String key);
}