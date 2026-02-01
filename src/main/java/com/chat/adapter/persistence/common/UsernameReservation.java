package com.chat.adapter.persistence.common;

import com.chat.application.port.out.UsernameReservationPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class UsernameReservation implements UsernameReservationPort {

    private final long ttlMillis;
    private final Map<String, Long> reservedUntil = new ConcurrentHashMap<>();
    private final Object lock = new Object();

    public UsernameReservation(@Value("${chat.username.reserve-ttl-seconds:60}") long ttlSeconds) {
        this.ttlMillis = ttlSeconds * 1000L;
    }

    @Override
    public boolean tryReserve(String username) {
        long now = System.currentTimeMillis();
        synchronized (lock) {
            Long until = reservedUntil.get(username);
            if (until != null && until > now) {
                return false; // déjà réservé et pas expiré
            }
            reservedUntil.put(username, now + ttlMillis);
            return true;
        }
    }

    @Override
    public void release(String username) {
        reservedUntil.remove(username);
    }
}