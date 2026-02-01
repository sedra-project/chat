package com.chat.application.dto;

public class ReactionToggleResult {
    private final String messageId;
    private final String emoji;
    private final int delta; // +1 ou -1

    public ReactionToggleResult(String messageId, String emoji, int delta) {
        this.messageId = messageId;
        this.emoji = emoji;
        this.delta = delta;
    }

    public String getMessageId() {
        return messageId;
    }

    public String getEmoji() {
        return emoji;
    }

    public int getDelta() {
        return delta;
    }
}
