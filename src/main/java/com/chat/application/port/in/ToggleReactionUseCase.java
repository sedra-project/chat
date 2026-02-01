package com.chat.application.port.in;

import com.chat.application.dto.ReactionToggleResult;

public interface ToggleReactionUseCase {
    ReactionToggleResult execute(String userId, String convId, String messageId, String emoji);
}
