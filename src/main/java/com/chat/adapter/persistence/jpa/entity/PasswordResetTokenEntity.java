package com.chat.adapter.persistence.jpa.entity;

import javax.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "password_reset_tokens",
        indexes = @Index(name = "idx_password_reset_email",
        columnList = "email"))
public class PasswordResetTokenEntity {
    @Id
    private String id;

    @Column(nullable = false, length = 255)
    private String email;

    @Column(nullable = false, length = 20)
    private String code;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant expiresAt;

    public PasswordResetTokenEntity() {
    }

    public PasswordResetTokenEntity(String id, String email, String code,
                                    Instant createdAt, Instant expiresAt) {
        this.id = id;
        this.email = email;
        this.code = code;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }
}
