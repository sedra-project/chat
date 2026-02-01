package com.chat.application.port.out;

import com.chat.domain.model.User;

import java.util.Optional;

public interface UserRepositoryPort {
    Optional<User> findById(String id);
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    void save(User user);
}