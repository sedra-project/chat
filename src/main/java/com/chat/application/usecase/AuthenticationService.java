package com.chat.application.usecase;


import com.chat.application.port.out.SessionTokenRepositoryPort;
import com.chat.application.port.out.UserRepositoryPort;
import com.chat.domain.model.Email;
import com.chat.domain.model.User;
import com.chat.domain.model.Username;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthenticationService {

    private final UserRepositoryPort userRepository;
    private final SessionTokenRepositoryPort sessionRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthenticationService(UserRepositoryPort userRepository,
                                 SessionTokenRepositoryPort sessionRepository,
                                 PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.sessionRepository = sessionRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public String register(String usernameRaw, String emailRaw, String rawPassword) {

        Username username = Username.of(usernameRaw);
        Email email = Email.of(emailRaw);

        if (userRepository.findByUsername(username.value()).isPresent()) {
            throw new RuntimeException("Ce pseudo est déjà pris.");
        }

        if (userRepository.findByEmail(email.value()).isPresent()) {
            throw new RuntimeException("Cet email est déjà utilisé.");
        }

        // 1. Sauvegarde du nouvel utilisateur
        String hash = passwordEncoder.encode(rawPassword);
        User user = User.createNew(username, email, hash);
        userRepository.save(user);

        // 2. AUTO-LOGIN : On crée et retourne directement le token
        return sessionRepository.createToken(user.getId());
    }

    public String login(String usernameRaw, String rawPassword) {
        Username username = Username.of(usernameRaw);

        User user = userRepository.findByUsername(username.value())
                .orElseThrow(() -> new RuntimeException("Utilisateur inconnu."));

        if (!passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
            throw new RuntimeException("Mot de passe incorrect.");
        }

        if(!user.isEnabled()){
            throw new RuntimeException("Compte desactivé.");
        }
        return sessionRepository.createToken(user.getId());
    }
}
