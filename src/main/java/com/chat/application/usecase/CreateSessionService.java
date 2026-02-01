package com.chat.application.usecase;

import com.chat.application.dto.SessionResult;
import com.chat.application.port.in.CreateSessionUseCase;
import com.chat.application.port.out.SessionTokenRepositoryPort;
import com.chat.application.port.out.UsernameReservationPort;
import com.chat.domain.exceptions.DuplicateUsernameException;
import com.chat.domain.model.Username;
import org.springframework.stereotype.Service;

@Service
public class CreateSessionService implements CreateSessionUseCase {

    private final SessionTokenRepositoryPort tokens;
    private final UsernameReservationPort reservations;

    public CreateSessionService(SessionTokenRepositoryPort tokens, UsernameReservationPort reservations) {
        this.tokens = tokens;
        this.reservations = reservations;
    }

    @Override
    public SessionResult create(String rawUsername) {
        Username username = Username.of(rawUsername); // validation domaine
        boolean ok = reservations.tryReserve(username.value());
        if (!ok) {
            throw new DuplicateUsernameException(username.value());
        }
        String token = tokens.createToken(username.value());
        return new SessionResult(token, username.value());
    }
}