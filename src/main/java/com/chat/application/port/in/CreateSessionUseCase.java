package com.chat.application.port.in;

import com.chat.application.dto.SessionResult;

public interface CreateSessionUseCase {
    SessionResult create(String rawUsername);
}