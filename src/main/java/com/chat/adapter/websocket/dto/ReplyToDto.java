package com.chat.adapter.websocket.dto;

public class ReplyToDto {
    private String messageId;
    private String senderUsername;
    private String excerpt;

    public ReplyToDto() {
    }

    public ReplyToDto(String messageId, String senderUsername, String excerpt) {
        this.messageId = messageId;
        this.senderUsername = senderUsername;
        this.excerpt = excerpt;
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public String getSenderUsername() {
        return senderUsername;
    }

    public void setSenderUsername(String senderUsername) {
        this.senderUsername = senderUsername;
    }

    public String getExcerpt() {
        return excerpt;
    }

    public void setExcerpt(String excerpt) {
        this.excerpt = excerpt;
    }
}