package com.chat.adapter.persistence.jpa.entity;

import javax.persistence.*;
import java.time.Instant;
import java.util.UUID;

/**
 * Entité JPA pour la table app_users.
 * Alignée sur le domaine User : id, username, email, passwordHash, enabled, createdAt, updatedAt.
 */
@Entity
@Table(
        name = "app_users",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_app_users_username", columnNames = "username"),
                @UniqueConstraint(name = "uk_app_users_email", columnNames = "email")
        },
        indexes = {
                @Index(name = "idx_app_users_email", columnList = "email")
        }
)
public class UserEntity {

    @Id
    @Column(length = 36)
    private String id;

    @Column(nullable = false, length = 50)
    private String username;

    @Column(nullable = false, length = 255)
    private String email;

    @Column(nullable = false, length = 255)
    private String passwordHash;

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    public UserEntity() {
    }

    public UserEntity(String id,
                      String username,
                      String email,
                      String passwordHash,
                      boolean enabled,
                      Instant createdAt,
                      Instant updatedAt) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.passwordHash = passwordHash;
        this.enabled = enabled;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    /**
     * Factory pratique pour un nouvel utilisateur côté persistance.
     */
    public static UserEntity newUser(String username, String email, String passwordHash) {
        Instant now = Instant.now();
        return new UserEntity(
                UUID.randomUUID().toString(),
                username,
                email,
                passwordHash,
                true,
                now,
                now
        );
    }

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        if (id == null) {
            id = UUID.randomUUID().toString();
        }
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    // --- Getters / Setters ---

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
