package com.chat.application.port.out;

import java.util.Optional;

public interface SessionTokenRepositoryPort {
    String createToken(String userId);

    Optional<String> findUserIdByToken(String token);

    void deleteToken(String token);
}