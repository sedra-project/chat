package com.chat.adapter.persistence.jpa.adapter.sessions;

import com.chat.adapter.persistence.jpa.entity.SessionEntity;
import com.chat.adapter.persistence.jpa.repository.sessions.SessionJpaRepository;
import com.chat.application.port.out.SessionTokenRepositoryPort;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@Profile("inDB")
public class DBSessionTokenRepository implements SessionTokenRepositoryPort {

    private final SessionJpaRepository jpaRepository;

    public DBSessionTokenRepository(SessionJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public String createToken(String username) {
        // 1. Génération d'un UUID unique
        String token = UUID.randomUUID().toString();

        // 2. Création de l'entité
        SessionEntity entity = new SessionEntity(token, username);

        // 3. Persistance en base
        jpaRepository.save(entity);

        // 4. Retour du token généré
        return token;
    }

    @Override
    public Optional<String> findUserIdByToken(String token) {
        return jpaRepository.findById(token)
                .map(SessionEntity::getUserId);
    }

    @Override
    public void deleteToken(String token) {
        if (token != null) {
            jpaRepository.deleteById(token);
        }
    }
}

