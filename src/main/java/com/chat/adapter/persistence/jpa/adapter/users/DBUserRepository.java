package com.chat.adapter.persistence.jpa.adapter.users;

import com.chat.adapter.persistence.jpa.entity.UserEntity;
import com.chat.adapter.persistence.jpa.mapper.UserJpaMapper;
import com.chat.adapter.persistence.jpa.repository.users.UserJpaRepository;
import com.chat.application.port.out.UserRepositoryPort;
import com.chat.domain.model.User;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Optional;

import static com.chat.adapter.persistence.jpa.mapper.UserJpaMapper.toEntity;
@Component
@Profile("inDB")
public class DBUserRepository implements UserRepositoryPort {

    private final UserJpaRepository userJpaRepository;

    public DBUserRepository(UserJpaRepository userJpaRepository) {
        this.userJpaRepository = userJpaRepository;
    }

    @Override
    public Optional<User> findById(String id) {
        return userJpaRepository.findById(id)
                .map(UserJpaMapper::toDomain);
    }

    @Override
    public Optional<User> findByUsername(String username) {
        return userJpaRepository.findByUsername(username)
                .map(UserJpaMapper::toDomain);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return userJpaRepository.findByEmail(email)
                .map(UserJpaMapper::toDomain);
    }

    @Override
    public void save(User user) {
        UserEntity entity = toEntity(user);
        userJpaRepository.save(entity);
    }
}
