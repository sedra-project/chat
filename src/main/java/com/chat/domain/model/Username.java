package com.chat.domain.model;

import com.chat.domain.exceptions.DomainException;

public final class Username {
    private static final String REGEX = "^[A-Za-z0-9_]{3,20}$";
    private final String value;

    private Username(String value) { this.value = value; }

    public static Username of(String raw) {
        if (raw == null) throw new DomainException("Username requis");
        String v = raw.trim();
        if (!v.matches(REGEX)) {
            throw new DomainException("Username invalide (3-20, alphanumérique + underscore)");
        }
        return new Username(v);
    }

    public String value() { return value; }
}