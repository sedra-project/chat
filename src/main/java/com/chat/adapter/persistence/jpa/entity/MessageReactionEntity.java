package com.chat.adapter.persistence.jpa.entity;

import javax.persistence.*;
import java.time.Instant;

@Entity
@Table(
        name = "message_reaction",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_message_reaction_msg_user_emoji",
                columnNames = {"message_id", "username", "emoji"}
        ),
        indexes = {
                @Index(name = "idx_message_reaction_message_id", columnList = "message_id")
        }
)
public class MessageReactionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "message_id", nullable = false, length = 64)
    private String messageId;

    @Column(name = "username", nullable = false, length = 50)
    private String username;

    @Column(name = "emoji", nullable = false, length = 10)
    private String emoji;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    public MessageReactionEntity() {
    }

    public MessageReactionEntity(String messageId, String username, String emoji) {
        this.messageId = messageId;
        this.username = username;
        this.emoji = emoji;
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmoji() {
        return emoji;
    }

    public void setEmoji(String emoji) {
        this.emoji = emoji;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
