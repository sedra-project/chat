package com.chat.adapter.persistence.jpa.repository.conversations;

import com.chat.adapter.persistence.jpa.entity.ConversationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ConversationJpaRepository extends JpaRepository<ConversationEntity, String> {
    // Cherche toutes les conversations où un utilisateur est participant
    @Query("SELECT c FROM ConversationEntity c JOIN c.participants p WHERE p = :username")
    List<ConversationEntity> findByParticipant(@Param("username") String username);
}
