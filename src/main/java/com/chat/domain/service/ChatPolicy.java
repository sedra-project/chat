package com.chat.domain.service;

import com.chat.domain.exceptions.DomainException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ChatPolicy {

    private final int maxLength;

    public ChatPolicy(@Value("${chat.message.max-length:500}") int maxLength) {
        this.maxLength = maxLength;
    }

    public String normalizeAndValidate(String content) {
        if (content == null) throw new DomainException("Message requis");
        String c = content.trim();
        if (c.isEmpty()) throw new DomainException("Message vide");
        if (c.length() > maxLength) throw new DomainException("Message trop long (max " + maxLength + ")");
        return c;
    }
}