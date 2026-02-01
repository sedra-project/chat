package com.chat.adapter.persistence.jpa.repository.passwords.reset;

import com.chat.adapter.persistence.jpa.entity.PasswordResetTokenEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Optional;

public interface PasswordResetTokenJpaRepository extends JpaRepository<PasswordResetTokenEntity, String> {

    Optional<PasswordResetTokenEntity> findTopByEmailOrderByCreatedAtDesc(String email);

    void deleteByEmail(String email);

    long deleteByExpiresAtBefore(Instant instant);
}
