package com.chat.domain.model;

import java.time.Instant;
import java.util.UUID;

public class PasswordResetToken {

    private final String id;
    private final String email;
    private final String code;       // code hexadécimal
    private final Instant createdAt;
    private final Instant expiresAt;

    public PasswordResetToken(String id, String email, String code,
                              Instant createdAt, Instant expiresAt) {
        this.id = id;
        this.email = email;
        this.code = code;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
    }

    public static PasswordResetToken generate(String email, long ttlMinutes) {
        Instant now = Instant.now();
        Instant exp = now.plusSeconds(ttlMinutes * 60);
        return new PasswordResetToken(
                UUID.randomUUID().toString(),
                email,
                generateHexCode(8),
                now,
                exp
        );
    }

    private static String generateHexCode(int length) {
        // Génération pseudo-aléatoire hexadécimale
        String s = UUID.randomUUID().toString().replace("-", "");
        return s.substring(0, Math.min(length, s.length())).toUpperCase();
    }

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    public String getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getCode() {
        return code;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }
}
