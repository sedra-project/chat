package com.chat.domain.model;

import java.util.Objects;

public class ReplyTo {
    private final String messageId;
    private final String senderUsername;
    private final String excerpt;

    public ReplyTo(String messageId, String senderUsername, String excerpt) {
        this.messageId = Objects.requireNonNull(messageId, "messageId requis");
        this.senderUsername = senderUsername;
        this.excerpt = excerpt;
    }

    public String getMessageId() {
        return messageId;
    }

    public String getSenderUsername() {
        return senderUsername;
    }

    public String getExcerpt() {
        return excerpt;
    }
}