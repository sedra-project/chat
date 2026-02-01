package com.chat.adapter.websocket.dto.requests;

public class ReactionRequest {
    private String messageId;
    private String emoji;
    public String getMessageId() {
        return messageId;
    }

    public String getEmoji() {
        return emoji;
    }
    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public void setEmoji(String emoji) {
        this.emoji = emoji;
    }
}
