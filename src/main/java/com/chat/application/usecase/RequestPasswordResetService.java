package com.chat.application.usecase;

import com.chat.application.port.in.RequestPasswordResetUseCase;
import com.chat.application.port.out.EmailSenderPort;
import com.chat.application.port.out.PasswordResetTokenRepositoryPort;
import com.chat.application.port.out.UserRepositoryPort;
import com.chat.domain.model.PasswordResetToken;
import com.chat.domain.model.User;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class RequestPasswordResetService implements RequestPasswordResetUseCase {

    private static final long TOKEN_TTL_MINUTES = 15;

    private final UserRepositoryPort userRepository;
    private final PasswordResetTokenRepositoryPort tokenRepository;
    private final EmailSenderPort emailSender;

    public RequestPasswordResetService(UserRepositoryPort userRepository,
                                       PasswordResetTokenRepositoryPort tokenRepository,
                                       EmailSenderPort emailSender) {
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.emailSender = emailSender;
    }

    @Override
    public void requestReset(String email) {
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (!userOpt.isPresent()) {
            // Ne pas révéler que l'email n'existe pas
            return;
        }

        // Supprimer les éventuels anciens tokens pour cet email
        tokenRepository.deleteByEmail(email);

        // Générer un nouveau token
        PasswordResetToken token = PasswordResetToken.generate(email, TOKEN_TTL_MINUTES);
        tokenRepository.save(token);

        // Envoyer l'email
        String subject = "Réinitialisation de votre mot de passe";
        String content = "Bonjour,\n\n" +
                "Votre code de réinitialisation est : " + token.getCode() + "\n" +
                "Il est valide pendant " + TOKEN_TTL_MINUTES + " minutes.\n\n" +
                "Si vous n'êtes pas à l'origine de cette demande, ignorez ce message.\n\n" +
                "Cordialement,\nL'équipe Chat Antsika";
        emailSender.sendEmail(email, subject, content);
    }
}