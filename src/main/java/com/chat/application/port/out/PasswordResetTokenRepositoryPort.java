package com.chat.application.port.out;

import com.chat.domain.model.PasswordResetToken;

import java.util.Optional;

public interface PasswordResetTokenRepositoryPort {

    PasswordResetToken save(PasswordResetToken token);

    Optional<PasswordResetToken> findValidTokenByEmail(String email);

    void deleteByEmail(String email);

    void delete(String id);
}