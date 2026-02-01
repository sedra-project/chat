package com.chat.adapter.persistence.jpa.adapter.passwords;

import com.chat.adapter.persistence.jpa.entity.PasswordResetTokenEntity;
import com.chat.adapter.persistence.jpa.repository.passwords.reset.PasswordResetTokenJpaRepository;
import com.chat.application.port.out.PasswordResetTokenRepositoryPort;
import com.chat.domain.model.PasswordResetToken;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Optional;
@Component
@Profile("inDB")
public class DBPasswordResetTokenRepository implements PasswordResetTokenRepositoryPort {

    private final PasswordResetTokenJpaRepository passwordResetTokenJpaRepository;

    public DBPasswordResetTokenRepository(PasswordResetTokenJpaRepository passwordResetTokenJpaRepository) {
        this.passwordResetTokenJpaRepository = passwordResetTokenJpaRepository;
    }

    @Override
    public PasswordResetToken save(PasswordResetToken token) {
        PasswordResetTokenEntity entity = new PasswordResetTokenEntity(
                token.getId(),
                token.getEmail(),
                token.getCode(),
                token.getCreatedAt(),
                token.getExpiresAt()
        );
        passwordResetTokenJpaRepository.save(entity);
        return token;
    }

    @Override
    public Optional<PasswordResetToken> findValidTokenByEmail(String email) {
        return passwordResetTokenJpaRepository.findTopByEmailOrderByCreatedAtDesc(email)
                .map(e -> new PasswordResetToken(
                        e.getId(),
                        e.getEmail(),
                        e.getCode(),
                        e.getCreatedAt(),
                        e.getExpiresAt()
                ))
                .filter(t -> !t.isExpired());
    }

    @Override
    public void deleteByEmail(String email) {
        passwordResetTokenJpaRepository.deleteByEmail(email);
    }

    @Override
    public void delete(String id) {
        passwordResetTokenJpaRepository.deleteById(id);
    }
}
