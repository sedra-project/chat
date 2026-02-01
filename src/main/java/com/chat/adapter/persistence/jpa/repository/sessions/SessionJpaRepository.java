package com.chat.adapter.persistence.jpa.repository.sessions;

import com.chat.adapter.persistence.jpa.entity.SessionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SessionJpaRepository extends JpaRepository<SessionEntity, String> {
    // findById (qui cherche par token) est déjà inclus dans JpaRepository
}
