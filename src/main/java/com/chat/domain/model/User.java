package com.chat.domain.model;

import com.chat.domain.exceptions.DomainException;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Entité domaine représentant un utilisateur de l'application.
 * Pas d'annotations JPA ici : la persistance se fait via un adapter.
 */
public class User {

    private final String id;
    private Username username;
    private Email email;
    private String passwordHash;
    private boolean enabled;
    private final Instant createdAt;
    private Instant updatedAt;

    public User(String id,
                Username username,
                Email email,
                String passwordHash,
                boolean enabled,
                Instant createdAt,
                Instant updatedAt) {

        this.id = id;
        this.username = Objects.requireNonNull(username, "username requis");
        this.email = Objects.requireNonNull(email, "email requis");
        this.passwordHash = Objects.requireNonNull(passwordHash, "passwordHash requis");
        this.enabled = enabled;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
        this.updatedAt = updatedAt != null ? updatedAt : this.createdAt;
    }

    /**
     * Factory pour un nouvel utilisateur.
     */
    public static User createNew(Username username, Email email, String passwordHash) {
        if (passwordHash == null || passwordHash.trim().isEmpty()) {
            throw new DomainException("Mot de passe requis");
        }
        Instant now = Instant.now();
        return new User(
                UUID.randomUUID().toString(),
                username,
                email,
                passwordHash,
                true,
                now,
                now
        );
    }

    // --- Métier ---

    public void changePasswordHash(String newHash) {
        if (newHash == null || newHash.trim().isEmpty()) {
            throw new DomainException("Nouveau mot de passe invalide");
        }
        this.passwordHash = newHash.trim();
        this.updatedAt = Instant.now();
    }

    public void changeEmail(Email newEmail) {
        this.email = Objects.requireNonNull(newEmail, "email requis");
        this.updatedAt = Instant.now();
    }

    public void disable() {
        this.enabled = false;
        this.updatedAt = Instant.now();
    }

    public void enable() {
        this.enabled = true;
        this.updatedAt = Instant.now();
    }

    // --- Getters ---

    public String getId() {
        return id;
    }

    public Username getUsername() {
        return username;
    }

    public Email getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    // --- Egalité basée sur l'id ---

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof User)) return false;
        User other = (User) o;
        return Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}