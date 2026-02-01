package com.chat.adapter.websocket.dto;

import com.chat.application.dto.ReactionToggleResult;
import com.chat.domain.model.Reaction;

public class ReactionResponseDto {
    private String messageId;
    private String emoji;
    private int delta;

    /**
     * Utilisé pour l'état initial/historique : chaque Reaction existante compte pour 1.
     */
    public static ReactionResponseDto from(Reaction reaction) {
        ReactionResponseDto dto = new ReactionResponseDto();
        dto.messageId = reaction.getMessageId();
        dto.emoji = reaction.getEmoji();
        dto.delta = 1;
        return dto;
    }

    /**
     * Utilisé pour l'événement temps réel (toggle) : delta = +1 ou -1.
     */
    public static ReactionResponseDto from(ReactionToggleResult result) {
        ReactionResponseDto dto = new ReactionResponseDto();
        dto.messageId = result.getMessageId();
        dto.emoji = result.getEmoji();
        dto.delta = result.getDelta();
        return dto;
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