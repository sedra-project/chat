package com.chat.application.dto;

public class ReactionEventDTO {
    private String messageId;
    private String emoji;
    private int delta; // +1 (ajout) ou -1 (retrait)

    public ReactionEventDTO() {
    }

    public ReactionEventDTO(String messageId, String emoji, int delta) {
        this.messageId = messageId;
        this.emoji = emoji;
        this.delta = delta;
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

    public int getDelta() {
        return delta;
    }

    public void setDelta(int delta) {
        this.delta = delta;
    }
}
