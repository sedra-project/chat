package com.chat.application.dto;

public class ReactionCommandDTO {
    private String messageId;
    private String emoji;

    public ReactionCommandDTO() {
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public String getEmoji() {
        return emoji;
    }

    public void setEmoji(String emoji) {
        this.emoji = emoji;
    }
}
