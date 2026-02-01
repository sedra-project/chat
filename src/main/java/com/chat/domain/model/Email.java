package com.chat.domain.model;

import com.chat.domain.exceptions.DomainException;

import java.util.Objects;
import java.util.regex.Pattern;

public final class Email {

    private static final Pattern REGEX =
            Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    private final String value;

    private Email(String value) {
        this.value = value;
    }

    public static Email of(String raw) {
        if (raw == null) throw new DomainException("Adresse e-mail requise");
        String v = raw.trim();
        if (v.isEmpty()) throw new DomainException("Adresse e-mail requise");
        if (!REGEX.matcher(v).matches()) {
            throw new DomainException("Adresse e-mail invalide");
        }
        return new Email(v.toLowerCase());
    }

    public String value() {
        return value;
    }

    @Override
    public String toString() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Email)) return false;
        Email email = (Email) o;
        return value.equals(email.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }
}