package com.chat.domain.model;

public class Reaction {
    private String id;
    private String messageId;
    private String userId;
    private String username;
    private String emoji;
    private long timestamp;

    public Reaction() {
    }

    public Reaction(String id, String messageId, String userId, String username, String emoji, long timestamp) {
        this.id = id;
        this.messageId = messageId;
        this.userId = userId;
        this.username = username;
        this.emoji = emoji;
        this.timestamp = timestamp;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
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

    public String getEmoji() {
        return emoji;
    }

    public void setEmoji(String emoji) {
        this.emoji = emoji;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}