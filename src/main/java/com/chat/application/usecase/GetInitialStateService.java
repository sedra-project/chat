package com.chat.application.usecase;

import com.chat.application.dto.InitialState;
import com.chat.application.port.in.GetInitialStateQuery;
import com.chat.application.port.out.ActiveUserRepositoryPort;
import com.chat.application.port.out.MessageRepositoryPort;
import com.chat.application.port.out.SessionTokenRepositoryPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class GetInitialStateService implements GetInitialStateQuery {

    private final SessionTokenRepositoryPort tokens;
    private final ActiveUserRepositoryPort activeUsers;
    private final MessageRepositoryPort messages;
    private final int recentSize;

    public GetInitialStateService(SessionTokenRepositoryPort tokens,
                                  ActiveUserRepositoryPort activeUsers,
                                  MessageRepositoryPort messages,
                                  @Value("${chat.message.recent-size:100}") int recentSize) {
        this.tokens = tokens;
        this.activeUsers = activeUsers;
        this.messages = messages;
        this.recentSize = recentSize;
    }

    @Override
    public InitialState get(String token) {
        // Authentification minimale: exige un token valide
        if (!tokens.findUserIdByToken(token).isPresent()) {
            throw new IllegalStateException("Unauthorized");
        }
        return new InitialState(activeUsers.list(), messages.recent(recentSize));
    }
}