package com.chat.adapter.websocket.mapper;

import com.chat.adapter.websocket.dto.ReplyToDto;
import com.chat.domain.model.ReplyTo;

public final class ReplyToMapper {
    private ReplyToMapper() {}

    public static ReplyTo toDomainOrNull(ReplyToDto dto) {
        if (dto == null) return null;
        if (dto.getMessageId() == null || dto.getMessageId().trim().isEmpty()) return null;
        return new ReplyTo(dto.getMessageId().trim(), dto.getSenderUsername(), dto.getExcerpt());
    }
}