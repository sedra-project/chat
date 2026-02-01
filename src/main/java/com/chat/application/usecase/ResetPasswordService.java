package com.chat.application.usecase;

import com.chat.application.port.in.ResetPasswordUseCase;
import com.chat.application.port.out.PasswordResetTokenRepositoryPort;
import com.chat.application.port.out.UserRepositoryPort;
import com.chat.domain.model.PasswordResetToken;
import com.chat.domain.model.User;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class ResetPasswordService implements ResetPasswordUseCase {

    private final UserRepositoryPort userRepository;
    private final PasswordResetTokenRepositoryPort tokenRepository;
    private final PasswordEncoder passwordEncoder;

    public ResetPasswordService(UserRepositoryPort userRepository,
                                PasswordResetTokenRepositoryPort tokenRepository,
                                PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void resetPassword(String email, String code, String newPassword) {
        PasswordResetToken token = tokenRepository.findValidTokenByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Code invalide ou expiré"));

        if (token.isExpired()) {
            tokenRepository.delete(token.getId());
            throw new IllegalArgumentException("Code expiré");
        }

        if (!token.getCode().equalsIgnoreCase(code.trim())) {
            throw new IllegalArgumentException("Code invalide");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur introuvable"));

        // Mise à jour du mot de passe (BCrypt)
        user.changePasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        // Consommer le token
        tokenRepository.delete(token.getId());
    }
}