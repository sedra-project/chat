package com.chat.adapter.persistence.jpa.mapper;

import com.chat.adapter.persistence.jpa.entity.UserEntity;
import com.chat.domain.model.Email;
import com.chat.domain.model.User;
import com.chat.domain.model.Username;

public class UserJpaMapper {
    public static User toDomain(UserEntity e) {
        return new User(
                e.getId(),
                Username.of(e.getUsername()),
                Email.of(e.getEmail()),
                e.getPasswordHash(),
                e.isEnabled(),
                e.getCreatedAt(),
                e.getUpdatedAt()
        );
    }

    public static UserEntity toEntity(User u) {
        UserEntity userEntity = new UserEntity();
        userEntity.setId(u.getId());
        userEntity.setUsername(u.getUsername().value());
        userEntity.setEmail(u.getEmail().value());
        userEntity.setPasswordHash(u.getPasswordHash());
        userEntity.setEnabled(u.isEnabled());
        userEntity.setCreatedAt(u.getCreatedAt());
        userEntity.setUpdatedAt(u.getUpdatedAt());
        return userEntity;
    }
}
