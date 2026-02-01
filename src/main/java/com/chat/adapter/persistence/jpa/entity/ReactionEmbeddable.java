package com.chat.adapter.persistence.jpa.entity;

import javax.persistence.Column;
import javax.persistence.Embeddable;

@Embeddable
public class ReactionEmbeddable {

    @Column(length = 16)
    private String emoji;

    @Column(name = "user_id", length = 36, nullable = false)
    private String userId;

    @Column(length = 50, nullable = false)
    private String username;

    private long timestamp;

    public ReactionEmbeddable() {
    }

    public ReactionEmbeddable(String emoji, String userId, String username, long timestamp) {
        this.emoji = emoji;
        this.userId = userId;
        this.username = username;
        this.timestamp = timestamp;
    }

    public String getEmoji() {
        return emoji;
    }

    public void setEmoji(String emoji) {
        this.emoji = emoji;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}